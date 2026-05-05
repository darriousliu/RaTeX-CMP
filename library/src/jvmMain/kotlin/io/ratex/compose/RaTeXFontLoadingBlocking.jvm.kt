package io.ratex.compose

import io.ratex.RaTeXFontLoader
import kotlinx.coroutines.runBlocking

internal actual fun ensureRaTeXFontsLoadedBlocking() {
    runBlocking {
        RaTeXFontLoader.ensureLoaded()
    }
}
