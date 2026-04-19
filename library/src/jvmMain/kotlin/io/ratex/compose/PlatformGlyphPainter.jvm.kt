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


private val textPaint = Paint().apply {
    isAntiAlias = true
    mode = PaintMode.FILL
}

internal actual fun DrawScope.drawPlatformGlyph(
    glyph: DisplayItem.GlyphPath,
    fontSizePx: Float,
) {
    val typeface = RaTeXFontLoader.getPlatformTypeFace(glyph.font) ?: return
    val codePoint = glyph.charCode
    if (!Character.isValidCodePoint(codePoint)) return
    val text = String(Character.toChars(codePoint))
    val font = Font(typeface, fontSizePx * glyph.scale.toFloat()).apply {
        edging = FontEdging.SUBPIXEL_ANTI_ALIAS
        isSubpixel = true
        isLinearMetrics = true
    }
    drawIntoCanvas { canvas ->
        textPaint.color = glyph.color.toComposeColor().toArgb()
        canvas.nativeCanvas.drawString(
            text,
            glyph.x.em(fontSizePx),
            glyph.y.em(fontSizePx),
            font,
            textPaint,
        )
    }
}
