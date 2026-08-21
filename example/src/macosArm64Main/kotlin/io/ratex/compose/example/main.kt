package io.ratex.compose.example

import androidx.compose.ui.window.Window
import platform.AppKit.NSApp
import platform.AppKit.NSApplication

fun main() {
    NSApplication.sharedApplication()
    Window("RaTeX Native macOS") {
        RaTeXExampleApp()
    }
    NSApp?.run()
}
