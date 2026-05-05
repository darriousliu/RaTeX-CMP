package io.ratex.compose

internal actual fun ensureRaTeXFontsLoadedBlocking() {
    // Browser targets cannot synchronously block while Compose resources/WASM initialize.
}
