package io.ratex

import androidx.compose.ui.graphics.Color
import io.ratex.RaTeXEngine.parse
import io.ratex.RaTeXEngine.parseBlocking

class RaTeXException(message: String) : Exception(message)

/**
 * Entry point for RaTeX rendering.
 *
 * ```kotlin
 * val displayList = RaTeXEngine.parse("""\frac{-b \pm \sqrt{b^2-4ac}}{2a}""")
 * ```
 *
 * Note: [parse] is a suspend function; call it from a coroutine.
 * For one-shot calls from non-coroutine code, use [parseBlocking].
 */
internal expect object RaTeXEngine {
    /**
     * Parse [latex] and return a [DisplayList] decoded from the JSON result.
     * Runs on [kotlinx.coroutines.Dispatchers.Default].
     *
     * @param displayMode `true` (default) for display/block style; `false` for inline/text style.
     * @throws RaTeXException on parse or decode error.
     */
    suspend fun parse(
        latex: String,
        displayMode: Boolean = true,
        color: Color = Color.Black,
    ): DisplayList

    /**
     * Blocking variant of [parse]. Safe to call on any background thread.
     * **Do not call on the main thread** — use [parse] instead.
     *
     * @param displayMode `true` (default) for display/block style; `false` for inline/text style.
     * @throws RaTeXException on parse or decode error.
     */
    fun parseBlocking(
        latex: String,
        displayMode: Boolean = true,
        color: Color = Color.Black,
    ): DisplayList
}
