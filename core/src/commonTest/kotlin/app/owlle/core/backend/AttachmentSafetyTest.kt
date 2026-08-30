package app.owlle.core.backend

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AttachmentSafetyTest {

    @Test
    fun executablesAndScriptsAreRisky() {
        assertTrue(AttachmentSafety.isRisky("setup.exe"))
        assertTrue(AttachmentSafety.isRisky("run.bat"))
        assertTrue(AttachmentSafety.isRisky("tool.jar"))
        assertTrue(AttachmentSafety.isRisky("page.html"))
    }

    @Test
    fun matchingIsCaseInsensitive() {
        assertTrue(AttachmentSafety.isRisky("SETUP.EXE"))
        assertTrue(AttachmentSafety.isRisky("Script.Ps1"))
    }

    @Test
    fun doubleExtensionTricksAreCaughtByTheFinalExtension() {
        assertTrue(AttachmentSafety.isRisky("invoice.pdf.exe"))
        assertFalse(AttachmentSafety.isRisky("report.exe.pdf"))
    }

    @Test
    fun ordinaryDocumentsAreNotFlagged() {
        assertFalse(AttachmentSafety.isRisky("photo.jpg"))
        assertFalse(AttachmentSafety.isRisky("report.pdf"))
        assertFalse(AttachmentSafety.isRisky("data.xlsx"))
        assertFalse(AttachmentSafety.isRisky("README"))
    }
}
