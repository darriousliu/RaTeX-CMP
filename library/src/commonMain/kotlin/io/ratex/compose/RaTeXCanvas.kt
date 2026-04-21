package io.ratex.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import io.ratex.DisplayItem
import io.ratex.DisplayList
import io.ratex.PathCommand
import kotlin.math.max

fun DrawScope.drawDisplayList(
    displayList: DisplayList,
    fontSizePx: Float,
    drawGlyph: DrawScope.(DisplayItem.GlyphPath, Float) -> Unit = { _, _ -> },
) {
    displayList.items.forEach { item ->
        when (item) {
            is DisplayItem.GlyphPath -> drawGlyph(item, fontSizePx)
            is DisplayItem.Line -> drawDisplayLine(item, fontSizePx)
            is DisplayItem.Rect -> drawDisplayRect(item, fontSizePx)
            is DisplayItem.Path -> drawDisplayPath(item, fontSizePx)
        }
    }
}

private fun DrawScope.drawDisplayLine(
    line: DisplayItem.Line,
    fontSizePx: Float,
) {
    val thickness = max(0.5f, line.thickness.em(fontSizePx))
    val halfT = thickness / 2f
    val left = line.x.em(fontSizePx)
    val top = line.y.em(fontSizePx) - halfT
    val right = (line.x + line.width).em(fontSizePx)
    val bottom = line.y.em(fontSizePx) + halfT
    val color = line.color.composeColor
    if (line.dashed) {
        val dashLength = thickness * 3f
        drawLine(
            color = color,
            start = Offset(left, line.y.em(fontSizePx)),
            end = Offset(right, line.y.em(fontSizePx)),
            strokeWidth = thickness,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLength, dashLength), 0f),
        )
    } else {
        drawRect(
            color = color,
            topLeft = Offset(left, top),
            size = Size(right - left, bottom - top)
        )
    }
}

private fun DrawScope.drawDisplayRect(
    rect: DisplayItem.Rect,
    fontSizePx: Float,
) {
    drawRect(
        color = rect.color.composeColor,
        topLeft = Offset(rect.x.em(fontSizePx), rect.y.em(fontSizePx)),
        size = Size(
            width = rect.width.em(fontSizePx),
            height = rect.height.em(fontSizePx),
        ),
    )
}

private fun DrawScope.drawDisplayPath(
    pathItem: DisplayItem.Path,
    fontSizePx: Float,
) {
    val path = Path().apply {
        pathItem.commands.forEach { command ->
            when (command) {
                is PathCommand.MoveTo -> moveTo(
                    (pathItem.x + command.x).em(fontSizePx),
                    (pathItem.y + command.y).em(fontSizePx),
                )

                is PathCommand.LineTo -> lineTo(
                    (pathItem.x + command.x).em(fontSizePx),
                    (pathItem.y + command.y).em(fontSizePx),
                )

                is PathCommand.CubicTo -> cubicTo(
                    (pathItem.x + command.x1).em(fontSizePx),
                    (pathItem.y + command.y1).em(fontSizePx),
                    (pathItem.x + command.x2).em(fontSizePx),
                    (pathItem.y + command.y2).em(fontSizePx),
                    (pathItem.x + command.x).em(fontSizePx),
                    (pathItem.y + command.y).em(fontSizePx),
                )

                is PathCommand.QuadTo -> quadraticTo(
                    (pathItem.x + command.x1).em(fontSizePx),
                    (pathItem.y + command.y1).em(fontSizePx),
                    (pathItem.x + command.x).em(fontSizePx),
                    (pathItem.y + command.y).em(fontSizePx),
                )

                PathCommand.Close -> close()
            }
        }
    }

    if (pathItem.fill) {
        drawPath(
            path = path,
            color = pathItem.color.composeColor,
        )
    } else {
        drawPath(
            path = path,
            color = pathItem.color.composeColor,
            style = Stroke(width = 1f),
        )
    }
}

internal fun Double.em(fontSizePx: Float): Float = (this * fontSizePx).toFloat()