package io.ratex.compose

import androidx.compose.ui.graphics.drawscope.DrawScope
import io.ratex.DisplayItem

internal expect fun DrawScope.drawPlatformGlyph(
    glyph: DisplayItem.GlyphPath,
    fontSizePx: Float,
)
