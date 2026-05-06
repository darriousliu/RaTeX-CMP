package io.ratex

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RaTeXFontLoaderJvmTest {
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
            File("src/commonMain/composeResources/files/fonts/$fileName"),
            File("library/src/commonMain/composeResources/files/fonts/$fileName"),
        )
        return candidates.firstOrNull { it.isFile }?.readBytes()
            ?: error("Could not find test font $fileName")
    }
}
