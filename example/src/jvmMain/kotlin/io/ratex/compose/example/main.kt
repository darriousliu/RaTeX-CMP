package io.ratex.compose.example

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.runBlocking

fun main() = application {
    runBlocking {
        preloadExampleFormulaFonts()
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "RaTeX Desktop Example",
    ) {
        RaTeXExampleApp()
    }
}
