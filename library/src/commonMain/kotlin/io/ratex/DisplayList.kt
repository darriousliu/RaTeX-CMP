package io.ratex

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class DisplayList(
    val version: Int? = null,
    val width: Double,
    val height: Double,
    val depth: Double,
    val items: List<DisplayItem>,
)

// MARK: - Drawing commands (flat structure matching Rust serde tag = "type")
@Serializable
sealed class DisplayItem {
    @Serializable
    @SerialName("GlyphPath")
    data class GlyphPath(
        val x: Double,
        val y: Double,
        val scale: Double,
        val font: String,
        @SerialName("char_code") val charCode: Int,
        val commands: List<PathCommand> = emptyList(),
        val color: RaTeXColor,
    ) : DisplayItem()

    @Serializable
    @SerialName("Line")
    data class Line(
        val x: Double,
        val y: Double,
        val width: Double,
        val thickness: Double,
        val color: RaTeXColor,
        val dashed: Boolean = false,
    ) : DisplayItem()

    @Serializable
    @SerialName("Rect")
    data class Rect(
        val x: Double,
        val y: Double,
        val width: Double,
        val height: Double,
        val color: RaTeXColor,
    ) : DisplayItem()

    @Serializable
    @SerialName("Path")
    data class Path(
        val x: Double,
        val y: Double,
        val commands: List<PathCommand>,
        val fill: Boolean,
        val color: RaTeXColor,
    ) : DisplayItem()
}

@Serializable
sealed class PathCommand {
    @Serializable
    @SerialName("MoveTo")
    data class MoveTo(val x: Double, val y: Double) : PathCommand()

    @Serializable
    @SerialName("LineTo")
    data class LineTo(val x: Double, val y: Double) : PathCommand()

    @Serializable
    @SerialName("CubicTo")
    data class CubicTo(
        val x1: Double,
        val y1: Double,
        val x2: Double,
        val y2: Double,
        val x: Double,
        val y: Double,
    ) : PathCommand()

    @Serializable
    @SerialName("QuadTo")
    data class QuadTo(
        val x1: Double,
        val y1: Double,
        val x: Double,
        val y: Double,
    ) : PathCommand()

    @Serializable
    @SerialName("Close")
    data object Close : PathCommand()
}

@Serializable
data class RaTeXColor(
    val r: Float,
    val g: Float,
    val b: Float,
    val a: Float,
) {
    fun toComposeColor(): Color = Color(
        red = r.coerceIn(0f, 1f),
        green = g.coerceIn(0f, 1f),
        blue = b.coerceIn(0f, 1f),
        alpha = a.coerceIn(0f, 1f),
    )
}

internal val ratexJson = Json {
    ignoreUnknownKeys = true
    classDiscriminator = "type"
}

data class RaTeXMeasuredDisplayList(
    val widthPx: Float,
    val heightPx: Float,
    val depthPx: Float,
) {
    val totalHeightPx: Float get() = heightPx + depthPx
}

fun DisplayList.measure(fontSizePx: Float): RaTeXMeasuredDisplayList = RaTeXMeasuredDisplayList(
    widthPx = (width * fontSizePx).toFloat(),
    heightPx = (height * fontSizePx).toFloat(),
    depthPx = (depth * fontSizePx).toFloat(),
)