package app.owlle.core.backend

import app.owlle.core.model.SpecialUse
import kotlin.test.Test
import kotlin.test.assertEquals

class FolderMapperTest {

    @Test
    fun inboxIsDetectedByPathRegardlessOfCase() {
        assertEquals(SpecialUse.INBOX, FolderMapper.detect("INBOX", "INBOX", emptySet()))
        assertEquals(SpecialUse.INBOX, FolderMapper.detect("inbox", "inbox", emptySet()))
    }

    @Test
    fun specialUseAttributesWinOverNames() {
        // Gmail-style: localized or arbitrary display name, attribute is authoritative
        assertEquals(
            SpecialUse.SENT,
            FolderMapper.detect("[Gmail]/Изпратени", "Изпратени", setOf("\\Sent", "\\HasNoChildren")),
        )
        assertEquals(
            SpecialUse.TRASH,
            FolderMapper.detect("[Gmail]/Bin", "Bin", setOf("\\Trash")),
        )
        assertEquals(
            SpecialUse.ALL,
            FolderMapper.detect("[Gmail]/All Mail", "All Mail", setOf("\\All")),
        )
    }

    @Test
    fun attributeMatchingIsCaseInsensitive() {
        assertEquals(SpecialUse.JUNK, FolderMapper.detect("Junk", "Junk", setOf("\\JUNK")))
    }

    @Test
    fun nameHeuristicsCoverProvidersWithoutSpecialUse() {
        // Exchange Online style: no SPECIAL-USE attributes advertised
        assertEquals(SpecialUse.SENT, FolderMapper.detect("Sent Items", "Sent Items", emptySet()))
        assertEquals(SpecialUse.TRASH, FolderMapper.detect("Deleted Items", "Deleted Items", emptySet()))
        assertEquals(SpecialUse.JUNK, FolderMapper.detect("Junk E-Mail", "Junk E-Mail", emptySet()))
        assertEquals(SpecialUse.DRAFTS, FolderMapper.detect("Drafts", "Drafts", emptySet()))
    }

    @Test
    fun userFoldersStayCustom() {
        assertEquals(SpecialUse.CUSTOM, FolderMapper.detect("Bali Trip", "Bali Trip", emptySet()))
        assertEquals(SpecialUse.CUSTOM, FolderMapper.detect("Work/Receipts", "Receipts", setOf("\\HasNoChildren")))
        // A folder merely NAMED like a special one but nested stays heuristic-matched only on exact name
        assertEquals(SpecialUse.CUSTOM, FolderMapper.detect("Projects/Sentinel", "Sentinel", emptySet()))
    }
}
