package app.owlle.core.imap

import app.owlle.core.backend.FolderMapper
import app.owlle.core.backend.MailBackend
import app.owlle.core.backend.MailBackendException
import app.owlle.core.model.AttachmentMeta
import app.owlle.core.model.Envelope
import app.owlle.core.model.MailAccount
import app.owlle.core.model.MailFolder
import app.owlle.core.model.MessageContent
import app.owlle.core.model.OutgoingMessage
import jakarta.mail.AuthenticationFailedException
import jakarta.mail.FetchProfile
import jakarta.mail.Flags
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.Multipart
import jakarta.mail.Part
import jakarta.mail.Session
import jakarta.mail.Store
import jakarta.mail.UIDFolder
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import jakarta.mail.internet.MimeUtility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.angus.mail.imap.IMAPFolder
import java.io.File
import java.util.Properties

/**
 * IMAP transport over Jakarta/Angus Mail (JVM targets: desktop + Android).
 * The iOS transport is a later milestone (shared Rust core or
 * Network.framework expect/actual — see the architecture notes).
 */
class ImapBackend : MailBackend {

    private var store: Store? = null
    private var account: MailAccount? = null

    override suspend fun connect(account: MailAccount): Unit = withContext(Dispatchers.IO) {
        try {
            val protocol = if (account.useSsl) "imaps" else "imap"
            val props = Properties().apply {
                put("mail.store.protocol", protocol)
                put("mail.$protocol.connectiontimeout", "15000")
                put("mail.$protocol.timeout", "30000")
                if (!account.useSsl) put("mail.imap.starttls.enable", "true")
            }
            val session = Session.getInstance(props)
            store = session.getStore(protocol).also {
                it.connect(account.imapHost, account.imapPort, account.username, account.password)
            }
            this@ImapBackend.account = account
        } catch (e: AuthenticationFailedException) {
            val serverSaysAppPassword = e.message?.contains("application-specific", ignoreCase = true) == true
            val hint = if (serverSaysAppPassword || account.imapHost.contains("gmail", ignoreCase = true)) {
                "Gmail rejected the sign-in: it requires an app password instead of your normal one. " +
                    "Create one at myaccount.google.com/apppasswords (needs 2-Step Verification), " +
                    "then paste the 16-character code here."
            } else {
                "The server rejected this username or password."
            }
            throw MailBackendException(hint, e)
        } catch (e: Exception) {
            throw MailBackendException(e.message ?: "Could not connect to ${account.imapHost}", e)
        }
    }

    override suspend fun folders(): List<MailFolder> = withContext(Dispatchers.IO) {
        val store = requireStore()
        try {
            store.defaultFolder.list("*")
                .filter { it.type and Folder.HOLDS_MESSAGES != 0 }
                .map { folder ->
                    val attributes = (folder as? IMAPFolder)?.attributes?.toSet() ?: emptySet()
                    MailFolder(
                        path = folder.fullName,
                        displayName = folder.name,
                        specialUse = FolderMapper.detect(folder.fullName, folder.name, attributes),
                    )
                }
                .distinctBy { it.path }
        } catch (e: Exception) {
            throw MailBackendException("Could not list folders: ${e.message}", e)
        }
    }

    override suspend fun envelopes(folder: MailFolder, limit: Int): List<Envelope> =
        withContext(Dispatchers.IO) {
            val imapFolder = openFolder(folder.path)
            try {
                val total = imapFolder.messageCount
                if (total == 0) return@withContext emptyList()

                val start = maxOf(1, total - limit + 1)
                val messages = imapFolder.getMessages(start, total)
                imapFolder.fetch(messages, FetchProfile().apply {
                    add(FetchProfile.Item.ENVELOPE)
                    add(FetchProfile.Item.FLAGS)
                    add(UIDFolder.FetchProfileItem.UID)
                })

                messages.mapNotNull { msg ->
                    val uid = imapFolder.getUID(msg)
                    if (uid < 0) return@mapNotNull null
                    val from = (msg.from?.firstOrNull() as? InternetAddress)
                    Envelope(
                        uid = uid,
                        subject = msg.subject ?: "(no subject)",
                        fromName = from?.personal ?: from?.address ?: "Unknown",
                        fromAddress = from?.address ?: "",
                        sentAtEpochMs = (msg.sentDate ?: msg.receivedDate)?.time ?: 0L,
                        seen = msg.flags.contains(Flags.Flag.SEEN),
                    )
                }.sortedByDescending { it.sentAtEpochMs }
            } catch (e: Exception) {
                throw MailBackendException("Could not load ${folder.displayName}: ${e.message}", e)
            } finally {
                imapFolder.closeQuietly()
            }
        }

    override suspend fun message(folder: MailFolder, uid: Long): MessageContent =
        withContext(Dispatchers.IO) {
            val imapFolder = openFolder(folder.path)
            try {
                val msg = imapFolder.getMessageByUID(uid)
                    ?: throw MailBackendException("Message no longer exists on the server")
                val from = (msg.from?.firstOrNull() as? InternetAddress)
                MessageContent(
                    uid = uid,
                    subject = msg.subject ?: "(no subject)",
                    fromDisplay = from?.let { addr ->
                        addr.personal?.let { "$it <${addr.address}>" } ?: addr.address
                    } ?: "Unknown",
                    toDisplay = msg.getRecipients(Message.RecipientType.TO)
                        ?.filterIsInstance<InternetAddress>()
                        ?.joinToString(", ") { it.personal ?: it.address }
                        ?: "",
                    sentAtEpochMs = (msg.sentDate ?: msg.receivedDate)?.time ?: 0L,
                    bodyText = extractBody(msg),
                    attachments = collectAttachments(msg),
                )
            } catch (e: MailBackendException) {
                throw e
            } catch (e: Exception) {
                throw MailBackendException("Could not load message: ${e.message}", e)
            } finally {
                imapFolder.closeQuietly()
            }
        }

    override suspend fun saveAttachment(
        folder: MailFolder,
        uid: Long,
        attachment: AttachmentMeta,
    ): String = withContext(Dispatchers.IO) {
        val imapFolder = openFolder(folder.path)
        try {
            val msg = imapFolder.getMessageByUID(uid)
                ?: throw MailBackendException("Message no longer exists on the server")
            val part = attachmentParts(msg).getOrNull(attachment.index)
                ?: throw MailBackendException("Attachment not found in this message")

            val downloads = File(System.getProperty("user.home"), "Downloads").apply { mkdirs() }
            val target = uniqueFile(downloads, attachment.name)
            part.inputStream.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            target.absolutePath
        } catch (e: MailBackendException) {
            throw e
        } catch (e: Exception) {
            throw MailBackendException("Could not save ${attachment.name}: ${e.message}", e)
        } finally {
            imapFolder.closeQuietly()
        }
    }

    override suspend fun attachmentBytes(
        folder: MailFolder,
        uid: Long,
        attachment: AttachmentMeta,
    ): ByteArray = withContext(Dispatchers.IO) {
        val imapFolder = openFolder(folder.path)
        try {
            val msg = imapFolder.getMessageByUID(uid)
                ?: throw MailBackendException("Message no longer exists on the server")
            val part = attachmentParts(msg).getOrNull(attachment.index)
                ?: throw MailBackendException("Attachment not found in this message")
            part.inputStream.use { it.readBytes() }
        } catch (e: MailBackendException) {
            throw e
        } catch (e: Exception) {
            throw MailBackendException("Could not load ${attachment.name}: ${e.message}", e)
        } finally {
            imapFolder.closeQuietly()
        }
    }

    override suspend fun send(message: OutgoingMessage): Unit = withContext(Dispatchers.IO) {
        val account = this@ImapBackend.account
            ?: throw MailBackendException("Not connected — add an account first")
        if (account.smtpHost.isBlank()) {
            throw MailBackendException("No SMTP server configured for this account")
        }
        try {
            val protocol = if (account.smtpSsl) "smtps" else "smtp"
            val props = Properties().apply {
                put("mail.$protocol.host", account.smtpHost)
                put("mail.$protocol.port", account.smtpPort.toString())
                put("mail.$protocol.auth", "true")
                put("mail.$protocol.connectiontimeout", "15000")
                put("mail.$protocol.timeout", "30000")
                if (!account.smtpSsl) put("mail.smtp.starttls.enable", "true")
            }
            val session = Session.getInstance(props)
            val mime = buildMime(session, account, message)
            val transport = session.getTransport(protocol)
            try {
                transport.connect(account.smtpHost, account.smtpPort, account.username, account.password)
                transport.sendMessage(mime, mime.allRecipients)
            } finally {
                runCatching { transport.close() }
            }
        } catch (e: MailBackendException) {
            throw e
        } catch (e: AuthenticationFailedException) {
            throw MailBackendException("The SMTP server rejected this username or password", e)
        } catch (e: Exception) {
            throw MailBackendException("Could not send: ${e.message}", e)
        }
    }

    override suspend fun saveDraft(
        message: OutgoingMessage,
        draftsFolder: MailFolder?,
    ): Unit = withContext(Dispatchers.IO) {
        val account = this@ImapBackend.account
            ?: throw MailBackendException("Not connected — add an account first")
        try {
            val mime = buildMime(Session.getInstance(Properties()), account, message)
            mime.setFlag(Flags.Flag.DRAFT, true)
            val folder = requireStore().getFolder(draftsFolder?.path ?: "Drafts")
            if (!folder.exists()) folder.create(Folder.HOLDS_MESSAGES)
            folder.appendMessages(arrayOf(mime))
        } catch (e: Exception) {
            throw MailBackendException("Could not save draft: ${e.message}", e)
        }
    }

    private fun buildMime(session: Session, account: MailAccount, out: OutgoingMessage): MimeMessage {
        val mime = MimeMessage(session)
        mime.setFrom(InternetAddress(account.email, account.displayName.ifBlank { null }, "UTF-8"))
        mime.setRecipients(Message.RecipientType.TO, InternetAddress.parse(out.to))
        if (out.cc.isNotBlank()) {
            mime.setRecipients(Message.RecipientType.CC, InternetAddress.parse(out.cc))
        }
        mime.setSubject(out.subject, "UTF-8")
        mime.sentDate = java.util.Date()
        if (out.attachmentPaths.isEmpty()) {
            mime.setText(out.body, "UTF-8")
        } else {
            val multipart = MimeMultipart()
            multipart.addBodyPart(MimeBodyPart().apply { setText(out.body, "UTF-8") })
            out.attachmentPaths.forEach { path ->
                multipart.addBodyPart(MimeBodyPart().apply { attachFile(File(path)) })
            }
            mime.setContent(multipart)
        }
        return mime
    }

    override suspend fun close(): Unit = withContext(Dispatchers.IO) {
        runCatching { store?.close() }
        store = null
    }

    // ---- internals -------------------------------------------------------

    private fun requireStore(): Store =
        store?.takeIf { it.isConnected }
            ?: throw MailBackendException("Not connected — add an account first")

    private fun openFolder(path: String): IMAPFolder {
        val folder = requireStore().getFolder(path) as? IMAPFolder
            ?: throw MailBackendException("Folder $path is not available over IMAP")
        folder.open(Folder.READ_ONLY)
        return folder
    }

    private fun Folder.closeQuietly() {
        runCatching { if (isOpen) close(false) }
    }

    private fun collectAttachments(msg: Part): List<AttachmentMeta> =
        attachmentParts(msg).mapIndexed { index, part ->
            AttachmentMeta(
                index = index,
                name = part.fileName?.let { runCatching { MimeUtility.decodeText(it) }.getOrDefault(it) }
                    ?: "attachment-${index + 1}",
                sizeBytes = part.size.toLong().coerceAtLeast(0L),
                mimeType = part.contentType?.substringBefore(';')?.trim()?.lowercase() ?: "application/octet-stream",
            )
        }

    /** All parts a user would call "attachments", in stable traversal order. */
    private fun attachmentParts(part: Part): List<Part> {
        val found = mutableListOf<Part>()
        fun walk(p: Part) {
            if (p.isMimeType("multipart/*")) {
                val multipart = p.content as? Multipart ?: return
                for (i in 0 until multipart.count) walk(multipart.getBodyPart(i))
                return
            }
            val isAttachment = Part.ATTACHMENT.equals(p.disposition, ignoreCase = true) ||
                (p.fileName != null && !p.isMimeType("text/plain") && !p.isMimeType("text/html"))
            if (isAttachment) found += p
        }
        walk(part)
        return found
    }

    private fun uniqueFile(dir: File, name: String): File {
        val safe = name.replace(Regex("""[/\\ ]"""), "_")
        var candidate = File(dir, safe)
        var n = 1
        val base = safe.substringBeforeLast('.', safe)
        val ext = safe.substringAfterLast('.', "")
        while (candidate.exists()) {
            candidate = File(dir, if (ext.isEmpty()) "$base ($n)" else "$base ($n).$ext")
            n++
        }
        return candidate
    }

    /** Prefer text/plain; fall back to naively de-tagged HTML. */
    private fun extractBody(part: Part): String {
        findText(part, "text/plain")?.let { return it.trim() }
        findText(part, "text/html")?.let { return HtmlText.strip(it).trim() }
        return "(no readable text part)"
    }

    private fun findText(part: Part, mimeType: String): String? {
        if (part.isMimeType(mimeType)) {
            return part.content as? String
        }
        if (part.isMimeType("multipart/*")) {
            val multipart = part.content as? Multipart ?: return null
            for (i in 0 until multipart.count) {
                findText(multipart.getBodyPart(i), mimeType)?.let { return it }
            }
        }
        return null
    }

}
