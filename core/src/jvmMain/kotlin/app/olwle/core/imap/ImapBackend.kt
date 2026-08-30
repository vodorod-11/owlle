package app.olwle.core.imap

import app.olwle.core.backend.FolderMapper
import app.olwle.core.backend.MailBackend
import app.olwle.core.backend.MailBackendException
import app.olwle.core.model.Envelope
import app.olwle.core.model.MailAccount
import app.olwle.core.model.MailFolder
import app.olwle.core.model.MessageContent
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.angus.mail.imap.IMAPFolder
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
                )
            } catch (e: MailBackendException) {
                throw e
            } catch (e: Exception) {
                throw MailBackendException("Could not load message: ${e.message}", e)
            } finally {
                imapFolder.closeQuietly()
            }
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

    /** Prefer text/plain; fall back to naively de-tagged HTML. */
    private fun extractBody(part: Part): String {
        findText(part, "text/plain")?.let { return it.trim() }
        findText(part, "text/html")?.let { return stripHtml(it).trim() }
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

    private fun stripHtml(html: String): String =
        html
            .replace(Regex("(?is)<(script|style)[^>]*>.*?</\\1>"), "")
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</p>"), "\n\n")
            .replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace(Regex("\n{3,}"), "\n\n")
}
