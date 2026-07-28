package io.ratex

import androidx.compose.ui.graphics.Color
import io.ratex.RaTeXEngine.parse

/**
 * Indicates that RaTeX could not parse, lay out, or decode a formula.
 *
 * @param message A platform-provided description of the failure.
 */
class RaTeXException(message: String) : Exception(message)

/**
 * Parses LaTeX source into platform-independent drawing commands.
 *
 * ```kotlin
 * val displayList = RaTeXEngine.parse("""\frac{-b \pm \sqrt{b^2-4ac}}{2a}""")
 * ```
 *
 * Parsing does not load rendering fonts and does not cache results. Cache and reuse the returned
 * [DisplayList] when the source, display mode, and color are unchanged. Compose callers can use
 * `rememberRaTeXDisplayList` to manage parsing and font loading with the composition lifecycle.
 */
expect object RaTeXEngine {
    /**
     * Parses and lays out [latex] on [kotlinx.coroutines.Dispatchers.Default].
     *
     * This function is intended for coroutine callers and executes parsing in
     * [kotlinx.coroutines.Dispatchers.Default]. On JVM and native targets, this moves blocking
     * parsing off the caller's context. Browser targets must finish RaTeX WASM initialization
     * before parsing, and the WASM call still executes on the browser event thread. The returned
     * drawing commands can be rendered repeatedly at different font sizes, but a change to
     * [displayMode] or [color] requires parsing again.
     *
     * @param latex The LaTeX formula source.
     * @param displayMode `true` (default) for display/block style; `false` for inline/text style.
     * @param color The color embedded in all generated drawing commands.
     * @return A display list whose dimensions and coordinates use font-size-relative units.
     * @throws RaTeXException If the browser engine is not initialized, or parsing, layout, or
     * result decoding fails.
     */
    suspend fun parse(
        latex: String,
        displayMode: Boolean = true,
        color: Color = Color.Black,
    ): DisplayList

    /**
     * Parses and lays out [latex] synchronously.
     *
     * On JVM and native targets, call this only from a background thread because parsing blocks
     * until the display list is ready. Prefer [parse] from coroutines. On browser targets this
     * function cannot initialize WASM synchronously and succeeds only after RaTeX has already been
     * initialized asynchronously. Like [parse], this function neither loads fonts nor caches the
     * result.
     *
     * @param latex The LaTeX formula source.
     * @param displayMode `true` (default) for display/block style; `false` for inline/text style.
     * @param color The color embedded in all generated drawing commands.
     * @return A display list whose dimensions and coordinates use font-size-relative units.
     * @throws RaTeXException If the engine is not initialized, or parsing, layout, or result
     * decoding fails.
     */
    fun parseBlocking(
        latex: String,
        displayMode: Boolean = true,
        color: Color = Color.Black,
    ): DisplayList
}
