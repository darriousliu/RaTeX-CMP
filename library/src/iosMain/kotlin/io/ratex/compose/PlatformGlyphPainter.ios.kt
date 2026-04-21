package io.ratex.compose

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import io.ratex.DisplayItem
import io.ratex.RaTeXFontLoader
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontEdging
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode

val textPaint = Paint().apply {
    isAntiAlias = true
    mode = PaintMode.FILL
}

internal actual fun DrawScope.drawPlatformGlyph(
    glyph: DisplayItem.GlyphPath,
    fontSizePx: Float,
) {
    val typeface = RaTeXFontLoader.getPlatformTypeFace(glyph.font) ?: return
    val text = glyph.charCode.toCodePointString() ?: return
    val font = Font(typeface, fontSizePx * glyph.scale.toFloat()).apply {
        edging = FontEdging.SUBPIXEL_ANTI_ALIAS
        isSubpixel = true
        isLinearMetrics = true
    }
    drawIntoCanvas { canvas ->
        textPaint.color = glyph.color.composeColor.toArgb()
        canvas.nativeCanvas.drawString(
            text,
            glyph.x.em(fontSizePx),
            glyph.y.em(fontSizePx),
            font,
            textPaint,
        )
    }
}

private fun Int.toCodePointString(): String? {
    if (this !in 0..0x10FFFF) return null
    if (this <= 0xFFFF) return toChar().toString()

    val surrogate = this - 0x10000
    val high = ((surrogate ushr 10) + 0xD800).toChar()
    val low = ((surrogate and 0x3FF) + 0xDC00).toChar()
    return charArrayOf(high, low).concatToString()
}
