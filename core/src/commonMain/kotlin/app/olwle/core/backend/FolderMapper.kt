package app.olwle.core.backend

import app.olwle.core.model.SpecialUse

/**
 * Maps a server folder onto olwle's sections: RFC 6154 special-use
 * attributes when the server advertises them (Gmail always does),
 * name heuristics as fallback (Exchange Online and many custom servers
 * don't advertise SPECIAL-USE). Per-account user overrides come later.
 */
object FolderMapper {

    private val attributeMap = mapOf(
        "\\sent" to SpecialUse.SENT,
        "\\drafts" to SpecialUse.DRAFTS,
        "\\trash" to SpecialUse.TRASH,
        "\\junk" to SpecialUse.JUNK,
        "\\archive" to SpecialUse.ARCHIVE,
        "\\all" to SpecialUse.ALL,
        "\\flagged" to SpecialUse.FLAGGED,
    )

    // Common English + provider spellings; extend with localized names as needed.
    private val nameHeuristics = listOf(
        SpecialUse.SENT to setOf("sent", "sent items", "sent messages", "sent mail"),
        SpecialUse.DRAFTS to setOf("drafts", "draft"),
        SpecialUse.TRASH to setOf("trash", "deleted", "deleted items", "deleted messages", "bin"),
        SpecialUse.JUNK to setOf("junk", "junk e-mail", "junk email", "spam", "bulk mail"),
        SpecialUse.ARCHIVE to setOf("archive", "archives", "all mail"),
    )

    fun detect(path: String, displayName: String, attributes: Set<String>): SpecialUse {
        if (path.equals("INBOX", ignoreCase = true)) return SpecialUse.INBOX

        val normalized = attributes.map { it.lowercase() }.toSet()
        for ((attr, use) in attributeMap) {
            if (attr in normalized) return use
        }

        val name = displayName.lowercase().trim()
        for ((use, names) in nameHeuristics) {
            if (name in names) return use
        }
        return SpecialUse.CUSTOM
    }
}
