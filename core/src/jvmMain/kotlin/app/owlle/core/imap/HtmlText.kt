package app.owlle.core.imap

/** Naive HTML→plain-text used until a real HTML renderer lands. */
internal object HtmlText {

    fun strip(html: String): String =
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
            .lines()
            .joinToString("\n") { it.replace(Regex("[ \t]{2,}"), " ").trim() }
            .replace(Regex("\n{3,}"), "\n\n")
}
