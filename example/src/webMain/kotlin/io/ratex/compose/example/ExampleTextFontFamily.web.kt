package io.ratex.compose.example

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import io.ratex.RaTeXFontLoader
import io.ratex.compose.example.resources.Res

private const val EXAMPLE_CJK_FONT_RESOURCE = "files/fonts/NotoSansCJKsc-Regular.ttf"
private const val EXAMPLE_CJK_FONT_IDENTITY = "RaTeX-Example-NotoSansCJKsc-Regular"

private const val EXAMPLE_EMOJI_FONT_RESOURCE = "files/fonts/NotoColorEmoji.ttf"
private const val EXAMPLE_EMOJI_FONT_IDENTITY = "RaTeX-Example-NotoColorEmoji"

private var exampleTextFontBytes: ByteArray? = null
private var exampleEmojiFontBytes: ByteArray? = null

suspend fun preloadExampleTextFonts() {
    if (exampleTextFontBytes == null) {
        exampleTextFontBytes = Res.readBytes(EXAMPLE_CJK_FONT_RESOURCE).also { bytes ->
            RaTeXFontLoader.registerCjkFallbackFont(bytes)
        }
    }
    if (exampleEmojiFontBytes == null) {
        exampleEmojiFontBytes = Res.readBytes(EXAMPLE_EMOJI_FONT_RESOURCE).also { bytes ->
            RaTeXFontLoader.registerEmojiFallbackFont(bytes)
        }
    }
}

@Composable
actual fun rememberExampleTextFontFamily(): FontFamily {
    val textBytes = exampleTextFontBytes
    val emojiBytes = exampleEmojiFontBytes
    return remember(textBytes, emojiBytes) {
        (textBytes to emojiBytes).let {
            val fonts = listOfNotNull(
                it.first?.let { Font(EXAMPLE_CJK_FONT_IDENTITY, it) },
                it.second?.let { Font(EXAMPLE_EMOJI_FONT_IDENTITY, it) }
            ).toTypedArray()
            if (fonts.isEmpty()) FontFamily.Default else FontFamily(*fonts)
        }
    }
}
