@file:OptIn(ExperimentalForeignApi::class)

package io.ratex

import androidx.compose.ui.graphics.Color
import kotlinx.cinterop.*
import ratex.*

internal actual object RaTeXFallbackBridge {
    actual suspend fun prepare() {
        if (ratex_cmp_bridge_version() != 1u) throw RaTeXException("Unsupported CMP metrics bridge version")
    }

    actual fun parse(latex: String, displayMode: Boolean, color: Color, metrics: RaTeXFallbackMetrics): String = memScoped {
        val values = metrics.entries.entries.toList()
        val entries = allocArray<RatexCmpGlyphMetrics>(values.size) { index ->
            code_point = values[index].key.toUInt()
            reserved = 0u
            width = values[index].value.widthEm
            height = values[index].value.heightEm
            depth = values[index].value.depthEm
        }
        val rgba = allocArray<FloatVar>(4)
        rgba[0] = color.red
        rgba[1] = color.green
        rgba[2] = color.blue
        rgba[3] = color.alpha
        val result = ratex_cmp_parse(latex, if (displayMode) 1 else 0, rgba, entries, values.size.convert())
            ?: throw RaTeXException(ratex_cmp_last_error()?.toKString() ?: "CMP metrics parse failed")
        try { result.toKString() } finally { ratex_cmp_free(result) }
    }
}
