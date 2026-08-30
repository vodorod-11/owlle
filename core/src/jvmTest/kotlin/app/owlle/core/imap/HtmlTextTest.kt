package app.owlle.core.imap

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HtmlTextTest {

    @Test
    fun stripsTagsAndKeepsText() {
        assertEquals("Hello World", HtmlText.strip("<p>Hello <b>World</b></p>").trim())
    }

    @Test
    fun dropsScriptAndStyleContentEntirely() {
        val out = HtmlText.strip("<style>.a{color:red}</style><script>alert(1)</script><p>Body</p>")
        assertFalse(out.contains("color:red"))
        assertFalse(out.contains("alert"))
        assertTrue(out.contains("Body"))
    }

    @Test
    fun decodesCommonEntities() {
        assertEquals("Q&A <ok> \"yes\"", HtmlText.strip("Q&amp;A &lt;ok&gt; &quot;yes&quot;").trim())
    }

    @Test
    fun brAndParagraphsBecomeLineBreaks() {
        val out = HtmlText.strip("line one<br>line two</p>after")
        assertTrue(out.contains("line one\nline two"))
    }

    @Test
    fun collapsesTableLayoutWhitespace() {
        val out = HtmlText.strip("<td>Global   Proxy</td>\n\n\n\n\n<td>   96   </td>")
        assertFalse(out.contains("   "), "runs of spaces should collapse")
        assertFalse(out.contains("\n\n\n"), "runs of blank lines should collapse")
    }
}
