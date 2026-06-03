package io.ratex.compose.example

import io.ratex.RaTeXFontLoader
import io.ratex.compose.example.resources.Res

private const val EXAMPLE_EMOJI_FONT_RESOURCE = "files/fonts/NotoColorEmoji.ttf"

private var exampleEmojiFontRegistered = false

suspend fun preloadExampleWebEmojiFont() {
    if (exampleEmojiFontRegistered) return
    exampleEmojiFontRegistered = RaTeXFontLoader.registerEmojiFallbackFont(
        Res.readBytes(EXAMPLE_EMOJI_FONT_RESOURCE),
    )
}
