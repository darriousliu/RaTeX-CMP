package io.ratex.compose.example

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import io.ratex.RaTeXFontLoader
import io.ratex.compose.example.resources.Res

private const val EXAMPLE_CJK_FONT_RESOURCE = "files/fonts/NotoSansCJKsc-Regular.ttf"
private const val EXAMPLE_CJK_FONT_IDENTITY = "RaTeX-Example-NotoSansCJKsc-Regular"

private var exampleTextFontBytes: ByteArray? = null

suspend fun preloadExampleTextFonts() {
    if (exampleTextFontBytes == null) {
        exampleTextFontBytes = Res.readBytes(EXAMPLE_CJK_FONT_RESOURCE).also { bytes ->
            RaTeXFontLoader.registerCjkFallbackFont(bytes)
        }
    }
}

@Composable
actual fun rememberExampleTextFontFamily(): FontFamily {
    val bytes = exampleTextFontBytes
    return remember(bytes) {
        bytes?.let {
            FontFamily(Font(EXAMPLE_CJK_FONT_IDENTITY, it))
        } ?: FontFamily.Default
    }
}
