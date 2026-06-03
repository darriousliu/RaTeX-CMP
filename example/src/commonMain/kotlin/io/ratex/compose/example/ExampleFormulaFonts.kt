package io.ratex.compose.example

import io.ratex.RaTeXFontLoader
import io.ratex.compose.example.resources.Res

private const val EXAMPLE_FORMULA_CJK_FONT_RESOURCE = "files/fonts/shanhaixuanwuti.ttf"

suspend fun preloadExampleFormulaFonts() {
    RaTeXFontLoader.ensureLoaded()
    RaTeXFontLoader.registerCjkFallbackFont(
        Res.readBytes(EXAMPLE_FORMULA_CJK_FONT_RESOURCE),
    )
}
