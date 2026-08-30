package app.owlle.core.model

/**
 * Runtime account description. The password lives in memory only for now;
 * platform keychain storage is a later milestone.
 */
data class MailAccount(
    val displayName: String,
    val email: String,
    val imapHost: String,
    val imapPort: Int = 993,
    val username: String,
    val password: String,
    val useSsl: Boolean = true,
)

/** RFC 6154 special-use roles, plus CUSTOM for user folders. */
enum class SpecialUse {
    INBOX, SENT, DRAFTS, TRASH, JUNK, ARCHIVE, ALL, FLAGGED, CUSTOM;

    /** The mandatory sections owlle always surfaces, in display order. */
    companion object {
        val pinnedOrder = listOf(INBOX, SENT, DRAFTS, JUNK, TRASH, ARCHIVE)
    }
}

data class MailFolder(
    val path: String,
    val displayName: String,
    val specialUse: SpecialUse,
)

/** Lightweight message header for list rendering; bodies are fetched lazily. */
data class Envelope(
    val uid: Long,
    val subject: String,
    val fromName: String,
    val fromAddress: String,
    val sentAtEpochMs: Long,
    val preview: String = "",
    val seen: Boolean = false,
)

data class MessageContent(
    val uid: Long,
    val subject: String,
    val fromDisplay: String,
    val toDisplay: String,
    val sentAtEpochMs: Long,
    val bodyText: String,
)
