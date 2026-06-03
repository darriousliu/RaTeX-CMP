package io.ratex

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RaTeXFontLoaderJvmTest {
    @Test
    fun registered_custom_misans_cjk_font_is_used_for_cjk_glyphs() {
        RaTeXFontLoader.clear()
        try {
            val cjkBytes = testFontBytes("shanhaixuanwuti.ttf")
            val codePoint = '中'.code

            assertEquals(2, RaTeXFontLoader.registerCjkFallbackFont(cjkBytes))
            val registeredTypeFace = assertNotNull(FontCache[FONT_ID_CJK_REGULAR])

            assertTrue(platformTypeFaceSupports(registeredTypeFace, codePoint))
            assertSame(
                registeredTypeFace,
                RaTeXFontLoader.getPlatformTypeFace(FONT_ID_CJK_REGULAR, codePoint),
            )

            val displayList = RaTeXEngine.parseBlocking(
                latex = """\text{中文}""",
                displayMode = true,
            )
            assertTrue(
                displayList.items.any { item ->
                    (item as? DisplayItem.GlyphPath)?.font == FONT_ID_CJK_REGULAR
                },
                "Expected CJK glyphs to use $FONT_ID_CJK_REGULAR",
            )
        } finally {
            RaTeXFontLoader.clear()
        }
    }

    @Test
    fun registered_emoji_fallback_is_used_when_cjk_font_lacks_code_point() {
        RaTeXFontLoader.clear()
        try {
            val cjkBytes = testFontBytes("KaTeX_Size1-Regular.ttf")
            val emojiBytes = testFontBytes("KaTeX_Main-Regular.ttf")
            val codePoint = 'A'.code

            assertTrue(RaTeXFontLoader.registerFont(FONT_ID_CJK_REGULAR, cjkBytes))
            assertTrue(RaTeXFontLoader.registerEmojiFallbackFont(emojiBytes))

            val cjkTypeFace = assertNotNull(FontCache[FONT_ID_CJK_REGULAR])
            val emojiTypeFace = assertNotNull(FontCache[FONT_ID_EMOJI_FALLBACK])

            assertFalse(platformTypeFaceSupports(cjkTypeFace, codePoint))
            assertTrue(platformTypeFaceSupports(emojiTypeFace, codePoint))
            assertSame(
                emojiTypeFace,
                RaTeXFontLoader.getPlatformTypeFace(FONT_ID_CJK_REGULAR, codePoint),
            )
        } finally {
            RaTeXFontLoader.clear()
        }
    }

    private fun testFontBytes(fileName: String): ByteArray {
        val candidates = listOf(
            File("src/jvmTest/resources/fonts/$fileName"),
            File("library/src/jvmTest/resources/fonts/$fileName"),
            File("src/commonMain/composeResources/files/fonts/$fileName"),
            File("library/src/commonMain/composeResources/files/fonts/$fileName"),
        )
        return candidates.firstOrNull { it.isFile }?.readBytes()
            ?: error("Could not find test font $fileName")
    }
}
