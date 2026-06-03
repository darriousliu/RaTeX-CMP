package io.ratex

import com.sun.jna.Memory
import com.sun.jna.Native
import java.nio.charset.Charset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RaTeXNativeEncodingJvmTest {
    @Test
    fun native_parse_uses_utf8_for_jna_string_arguments() {
        assertEquals("UTF-8", Native.getStringEncoding(RaTeXNative::class.java))

        val displayList = RaTeXEngine.parseBlocking(
            latex = """\text{中文}""",
            displayMode = true,
        )

        assertTrue(displayList.items.isNotEmpty())
        assertNotNull(
            displayList.items
                .mapNotNull { item -> (item as? DisplayItem.GlyphPath)?.font }
                .firstOrNull { font -> font == FONT_ID_CJK_REGULAR },
            "Expected CJK glyphs to parse through UTF-8 JNA string binding",
        )
    }

    @Test
    fun native_parse_reports_invalid_utf8_for_non_utf8_latex_c_string() {
        val gbkLatexBytes = """\text{中文}""".toByteArray(Charset.forName("GBK"))
        val latexCString = Memory((gbkLatexBytes.size + 1).toLong()).apply {
            write(0, gbkLatexBytes, 0, gbkLatexBytes.size)
            setByte(gbkLatexBytes.size.toLong(), 0)
        }

        val result = RaTeXNative.instance.ratex_parse_and_layout(latexCString, null)

        assertEquals(1, result.error_code)
        assertEquals(
            RaTeXNative.instance.ratex_get_last_error()?.contains("invalid UTF-8 in latex string"),
            true,
            "Expected native UTF-8 validation error",
        )
    }
}
