package app.olwle.core.backend

import app.olwle.core.model.Envelope
import app.olwle.core.model.MailAccount
import app.olwle.core.model.MailFolder
import app.olwle.core.model.MessageContent

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
    suspend fun close()
}

class MailBackendException(message: String, cause: Throwable? = null) : Exception(message, cause)
