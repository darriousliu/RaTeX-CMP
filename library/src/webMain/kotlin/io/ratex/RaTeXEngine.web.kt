@file:OptIn(ExperimentalWasmJsInterop::class)

package io.ratex

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise
import kotlin.math.roundToInt

actual object RaTeXEngine {
    private var initPromise: Promise<JsAny?>? = null
    private var initialized = false

    suspend fun initialize() {
        if (initialized) return
        val promise = initPromise ?: initRatex().also { initPromise = it }
        try {
            promise.await()
            initialized = true
        } catch (error: Throwable) {
            if (initPromise === promise) {
                initPromise = null
            }
            throw RaTeXException(error.message ?: "RaTeX WASM initialization failed")
        }
    }

    actual suspend fun parse(
        latex: String,
        displayMode: Boolean,
        color: Color,
    ): DisplayList = withContext(Dispatchers.Default) {
        parseInitialized(latex, displayMode, color)
    }

    actual fun parseBlocking(
        latex: String,
        displayMode: Boolean,
        color: Color,
    ): DisplayList {
        if (!initialized) {
            throw RaTeXException("RaTeX WASM is not initialized yet; call the suspend parse() API on web targets")
        }
        return parseInitialized(latex, displayMode, color)
    }

    private fun parseInitialized(
        latex: String,
        displayMode: Boolean,
        color: Color,
    ): DisplayList {
        val source = if (displayMode) latex else """\textstyle{$latex}"""
        val json = try {
            renderLatex(source, color.toRatexCssColor())
        } catch (error: Throwable) {
            throw RaTeXException(error.message ?: "RaTeX WASM parse failed")
        }
        return try {
            ratexJson.decodeFromString(DisplayList.serializer(), json)
        } catch (error: Exception) {
            throw RaTeXException("JSON decode failed: ${error.message}")
        }
    }
}

internal expect suspend fun <T : JsAny?> Promise<T>.await(): T

private fun Color.toRatexCssColor(): String {
    val red = (red.coerceIn(0f, 1f) * 255f).roundToInt().toString(16).padStart(2, '0')
    val green = (green.coerceIn(0f, 1f) * 255f).roundToInt().toString(16).padStart(2, '0')
    val blue = (blue.coerceIn(0f, 1f) * 255f).roundToInt().toString(16).padStart(2, '0')
    return "#$red$green$blue"
}
