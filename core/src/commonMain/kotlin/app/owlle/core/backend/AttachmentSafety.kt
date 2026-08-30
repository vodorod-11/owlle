package app.owlle.core.backend

/**
 * Flags attachment types that can execute code or script when opened.
 * Flagged files are never blocked — the UI warns and asks before saving.
 * owlle itself only ever writes attachment bytes to disk; it never opens
 * or executes them.
 */
object AttachmentSafety {

    private val riskyExtensions = setOf(
        // executables & installers
        "exe", "msi", "com", "scr", "pif", "cpl", "app", "apk", "dmg", "pkg",
        // scripts
        "bat", "cmd", "js", "jse", "vbs", "vbe", "wsf", "wsh", "ps1", "psm1",
        "sh", "command", "hta", "jar",
        // containers that smuggle the above past filters
        "iso", "img", "vhd", "lnk", "reg",
        // markup that runs script when opened locally
        "html", "htm", "xhtml", "svg",
    )

    fun isRisky(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in riskyExtensions
    }
}
