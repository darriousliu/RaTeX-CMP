package io.ratex.compose.example

import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.runBlocking

fun MainViewController() = run {
    runBlocking { preloadExampleFormulaFonts() }
    ComposeUIViewController { RaTeXExampleApp() }
}