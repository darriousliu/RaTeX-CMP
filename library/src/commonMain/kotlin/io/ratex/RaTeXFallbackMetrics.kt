package io.ratex

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Advance, ascent and descent in em at the base font size, before TeX style scaling. */
data class FallbackGlyphMetrics(
    val widthEm: Double,
    val heightEm: Double,
    val depthEm: Double,
) {
    init {
        require(listOf(widthEm, heightEm, depthEm).all { it.isFinite() && it in 0.0..100.0 }) {
            "Fallback metrics must be finite and within 0..100 em"
        }
    }
}

/** Immutable, per-call metrics. Keys are Unicode scalar values, including non-BMP code points. */
class RaTeXFallbackMetrics private constructor(
    internal val entries: Map<Int, FallbackGlyphMetrics>,
) {
    val size: Int get() = entries.size

    companion object {
        val Empty = RaTeXFallbackMetrics(emptyMap())
        const val MaxEntries: Int = 4096

        /** Copies [entries]; later changes to the caller's map cannot affect parsing. */
        fun of(entries: Map<Int, FallbackGlyphMetrics>): RaTeXFallbackMetrics {
            require(entries.size <= MaxEntries) { "Fallback metrics exceed $MaxEntries entries" }
            require(entries.keys.all { it in 0..0x10FFFF && it !in 0xD800..0xDFFF }) {
                "Fallback metric keys must be valid Unicode scalar values"
            }
            return if (entries.isEmpty()) Empty else RaTeXFallbackMetrics(entries.toMap())
        }
    }
}

/**
 * Parses using host-provided metrics only for glyphs absent from KaTeX's metric tables.
 * No fonts are loaded. Render with the same font used to measure [fallbackMetrics].
 * An empty map uses the original parser, with exactly the same output and initialization rules.
 */
suspend fun RaTeXEngine.parseWithFallbackMetrics(
    latex: String,
    fallbackMetrics: RaTeXFallbackMetrics,
    displayMode: Boolean = true,
    color: Color = Color.Black,
): DisplayList {
    if (fallbackMetrics.size == 0) return parse(latex, displayMode, color)
    RaTeXFallbackBridge.prepare()
    return withContext(Dispatchers.Default) {
        parseBlockingWithFallbackMetrics(latex, fallbackMetrics, displayMode, color)
    }
}

/**
 * Synchronous counterpart of [parseWithFallbackMetrics]. On browsers call
 * [prepareFallbackMetrics] first. On native/JVM targets use a background thread.
 */
fun RaTeXEngine.parseBlockingWithFallbackMetrics(
    latex: String,
    fallbackMetrics: RaTeXFallbackMetrics,
    displayMode: Boolean = true,
    color: Color = Color.Black,
): DisplayList {
    if (fallbackMetrics.size == 0) return parseBlocking(latex, displayMode, color)
    require('\u0000' !in latex) { "LaTeX must not contain a null character" }
    val json = RaTeXFallbackBridge.parse(latex, displayMode, color, fallbackMetrics)
    return try {
        ratexJson.decodeFromString(DisplayList.serializer(), json)
    } catch (error: Exception) {
        throw RaTeXException("Fallback metrics JSON decode failed: ${error.message}")
    }
}

/** Loads the optional bridge. Browser initialization is retryable after a load failure. */
suspend fun RaTeXEngine.prepareFallbackMetrics() = RaTeXFallbackBridge.prepare()

internal expect object RaTeXFallbackBridge {
    suspend fun prepare()
    fun parse(latex: String, displayMode: Boolean, color: Color, metrics: RaTeXFallbackMetrics): String
}
