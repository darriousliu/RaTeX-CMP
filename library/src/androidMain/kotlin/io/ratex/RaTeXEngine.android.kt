package io.ratex

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Entry point for RaTeX rendering on Android.
 *
 * ```kotlin
 * val displayList = RaTeXEngine.parse("""\frac{-b \pm \sqrt{b^2-4ac}}{2a}""")
 * ```
 *
 * Note: [parse] is a suspend function; call it from a coroutine.
 * For one-shot calls from non-coroutine code, use [parseBlocking].
 */
internal actual object RaTeXEngine {
    init {
        System.loadLibrary("ratex_ffi")
    }

    // -------------------------------------------------------------------------
    // JNI declarations (implemented in crates/ratex-ffi/src/jni.rs)
    // -------------------------------------------------------------------------

    /**
     * Parse and lay out a LaTeX string with explicit display mode.
     * @param displayMode true = display/block style, false = inline/text style.
     * @return JSON DisplayList string on success, or null on error.
     */
    @JvmStatic
    private external fun nativeParseAndLayout(latex: String, displayMode: Boolean): String?

    /**
     * Retrieve the last error message produced by a native layout call on this thread.
     */
    @JvmStatic
    private external fun nativeGetLastError(): String?

    actual suspend fun parse(latex: String, displayMode: Boolean): DisplayList =
        withContext(Dispatchers.Default) { parseBlocking(latex, displayMode) }

    actual fun parseBlocking(latex: String, displayMode: Boolean): DisplayList {
        val json = nativeParseAndLayout(latex, displayMode)
            ?: throw RaTeXException(nativeGetLastError() ?: "unknown error")
        return try {
            ratexJson.decodeFromString(DisplayList.serializer(), json)
        } catch (e: Exception) {
            throw RaTeXException("JSON decode failed: ${e.message}")
        }
    }
}
