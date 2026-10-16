package io.ratex

import androidx.compose.ui.graphics.Color

internal actual object RaTeXFallbackBridge {
    private val loaded by lazy {
        try {
            System.loadLibrary("ratex_cmp_bridge")
        } catch (error: UnsatisfiedLinkError) {
            throw RaTeXException("Missing CMP metrics bridge; rebuild the Android native libraries: ${error.message}")
        }
    }

    @JvmStatic
    private external fun nativeParse(
        latex: String,
        displayMode: Boolean,
        rgba: FloatArray,
        codePoints: IntArray,
        dimensions: DoubleArray,
    ): String

    actual suspend fun prepare() { loaded }

    actual fun parse(latex: String, displayMode: Boolean, color: Color, metrics: RaTeXFallbackMetrics): String {
        loaded
        val entries = metrics.entries.entries.toList()
        return try {
            nativeParse(latex, displayMode, floatArrayOf(color.red, color.green, color.blue, color.alpha),
                entries.map { it.key }.toIntArray(),
                entries.flatMap { listOf(it.value.widthEm, it.value.heightEm, it.value.depthEm) }.toDoubleArray())
        } catch (error: UnsatisfiedLinkError) {
            throw RaTeXException("Incompatible CMP metrics bridge; rebuild the Android native libraries: ${error.message}")
        }
    }
}
