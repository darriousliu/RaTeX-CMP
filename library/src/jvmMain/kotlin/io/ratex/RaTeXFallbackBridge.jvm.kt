package io.ratex

import androidx.compose.ui.graphics.Color
import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.IntegerType
import com.sun.jna.Pointer

internal class RatexSizeT(value: Long = 0) : IntegerType(Native.SIZE_T_SIZE, value, true) {
    override fun toByte(): Byte = toLong().toByte()
    override fun toShort(): Short = toLong().toShort()
}

internal interface RaTeXMetricsNative : Library {
    fun ratex_cmp_bridge_version(): Int
    fun ratex_cmp_parse(latex: String, displayMode: Int, rgba: FloatArray, entries: Pointer, count: RatexSizeT): Pointer?
    fun ratex_cmp_free(json: Pointer)
    fun ratex_cmp_last_error(): String?
}

internal actual object RaTeXFallbackBridge {
    private val native by lazy {
        try {
            RaTeXNative.loadLibrary("ratex_cmp_bridge", RaTeXMetricsNative::class.java).also {
                if (it.ratex_cmp_bridge_version() != 1) throw RaTeXException("Unsupported CMP metrics bridge version")
            }
        } catch (error: UnsatisfiedLinkError) {
            throw RaTeXException("Missing or incompatible CMP metrics bridge; rebuild the desktop native libraries: ${error.message}")
        }
    }

    actual suspend fun prepare() { native }

    actual fun parse(latex: String, displayMode: Boolean, color: Color, metrics: RaTeXFallbackMetrics): String =
        Memory(maxOf(1L, metrics.size.toLong() * 32)).use { memory ->
            metrics.entries.entries.forEachIndexed { index, (codePoint, value) ->
                val offset = index.toLong() * 32
                memory.setInt(offset, codePoint)
                memory.setInt(offset + 4, 0)
                memory.setDouble(offset + 8, value.widthEm)
                memory.setDouble(offset + 16, value.heightEm)
                memory.setDouble(offset + 24, value.depthEm)
            }
            val ptr = native.ratex_cmp_parse(latex, if (displayMode) 1 else 0,
                floatArrayOf(color.red, color.green, color.blue, color.alpha), memory, RatexSizeT(metrics.size.toLong()))
                ?: throw RaTeXException(native.ratex_cmp_last_error() ?: "Fallback metrics parse failed")
            try { ptr.getString(0, "UTF-8") } finally { native.ratex_cmp_free(ptr) }
        }
}
