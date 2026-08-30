package app.owlle.core.backend

import app.owlle.core.model.AttachmentMeta
import app.owlle.core.model.Envelope
import app.owlle.core.model.MailAccount
import app.owlle.core.model.MailFolder
import app.owlle.core.model.MessageContent
import app.owlle.core.model.OutgoingMessage

/**
 * One mailbox transport. Phase-1 ships an IMAP implementation (JVM);
 * a Microsoft Graph implementation and a JMAP implementation slot in
 * behind this same interface later.
 */
interface MailBackend {
    suspend fun connect(account: MailAccount)
    suspend fun folders(): List<MailFolder>
    suspend fun envelopes(folder: MailFolder, limit: Int = 50): List<Envelope>
    suspend fun message(folder: MailFolder, uid: Long): MessageContent

    /** Streams one attachment to the platform's downloads directory; returns the saved path. */
    suspend fun saveAttachment(folder: MailFolder, uid: Long, attachment: AttachmentMeta): String

    /** Fetches one attachment's raw bytes (used for inline image previews). */
    suspend fun attachmentBytes(folder: MailFolder, uid: Long, attachment: AttachmentMeta): ByteArray

    /** Sends over SMTP with the account's submission settings. */
    suspend fun send(message: OutgoingMessage)

    /** Appends the message to the Drafts folder (created if the server has none). */
    suspend fun saveDraft(message: OutgoingMessage, draftsFolder: MailFolder?)

    suspend fun close()
}

class MailBackendException(message: String, cause: Throwable? = null) : Exception(message, cause)
