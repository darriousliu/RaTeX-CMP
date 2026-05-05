package io.ratex.compose.example

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import io.ratex.RaTeXEngine
import io.ratex.RaTeXFontLoader
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

private val mainScope = MainScope()

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    mainScope.launch {
        runCatching {
            RaTeXEngine.initialize()
            RaTeXFontLoader.ensureLoaded()
            preloadExampleTextFonts()
        }.onFailure { error ->
            println("RaTeX web preload failed: ${error.message}")
        }

        ComposeViewport {
            RaTeXExampleApp()
        }
    }
}
