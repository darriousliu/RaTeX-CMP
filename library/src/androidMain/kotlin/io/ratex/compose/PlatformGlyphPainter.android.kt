package io.ratex.compose

import android.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import io.ratex.DisplayItem
import io.ratex.RaTeXFontLoader

val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    isAntiAlias = true
    style = Paint.Style.FILL
}

internal actual fun DrawScope.drawPlatformGlyph(
    glyph: DisplayItem.GlyphPath,
    fontSizePx: Float,
) {
    val typeface = RaTeXFontLoader.getPlatformTypeFace(glyph.font) ?: return
    val codePoint = glyph.charCode
    if (!Character.isValidCodePoint(codePoint)) return
    val text = String(Character.toChars(codePoint))
    drawIntoCanvas { canvas ->
        textPaint.typeface = typeface
        textPaint.textSize = fontSizePx * glyph.scale.toFloat()
        textPaint.color = glyph.color.composeColor.toArgb()
        canvas.nativeCanvas.drawText(
            text,
            glyph.x.em(fontSizePx),
            glyph.y.em(fontSizePx),
            textPaint,
        )
    }
}
