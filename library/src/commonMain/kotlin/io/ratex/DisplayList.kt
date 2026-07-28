package io.ratex

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Platform-independent output produced by [RaTeXEngine].
 *
 * All dimensions and drawing coordinates are expressed in units relative to the requested font
 * size. A display list can therefore be cached and rendered at multiple font sizes without being
 * parsed again. The display mode and colors are already baked into [items]; parse again when either
 * changes.
 *
 * @property version Optional version of the serialized RaTeX display-list protocol.
 * @property width Horizontal advance of the formula, in font-size-relative units.
 * @property height Distance from the top of the formula to its baseline, in
 * font-size-relative units.
 * @property depth Distance from the baseline to the bottom of the formula, in
 * font-size-relative units.
 * @property items Drawing commands in paint order.
 */
@Serializable
data class DisplayList(
    val version: Int? = null,
    val width: Double,
    val height: Double,
    val depth: Double,
    val items: List<DisplayItem>,
)

// MARK: - Drawing commands (flat structure matching Rust serde tag = "type")
/**
 * A drawing command in a [DisplayList].
 *
 * Commands use font-size-relative coordinates and are ordered back-to-front in
 * [DisplayList.items].
 */
@Serializable
sealed class DisplayItem {
    /**
     * Draws one glyph from a RaTeX font.
     *
     * @property x Horizontal glyph origin in font-size-relative units.
     * @property y Vertical glyph origin in font-size-relative units.
     * @property scale Scale applied to the base font size for this glyph.
     * @property font RaTeX font identifier, such as `Main-Regular`.
     * @property charCode Unicode code point to draw.
     * @property color Glyph color.
     */
    @Serializable
    @SerialName("GlyphPath")
    data class GlyphPath(
        val x: Double,
        val y: Double,
        val scale: Double,
        val font: String,
        @SerialName("char_code")
        val charCode: Int,
        val color: RaTeXColor,
    ) : DisplayItem()

    /**
     * Draws a horizontal rule.
     *
     * @property x Horizontal start in font-size-relative units.
     * @property y Vertical center in font-size-relative units.
     * @property width Rule width in font-size-relative units.
     * @property thickness Rule thickness in font-size-relative units.
     * @property color Rule color.
     * @property dashed Whether to render the rule with a dash pattern.
     */
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

    /**
     * Draws a filled rectangle.
     *
     * @property x Horizontal origin in font-size-relative units.
     * @property y Vertical origin in font-size-relative units.
     * @property width Rectangle width in font-size-relative units.
     * @property height Rectangle height in font-size-relative units.
     * @property color Fill color.
     */
    @Serializable
    @SerialName("Rect")
    data class Rect(
        val x: Double,
        val y: Double,
        val width: Double,
        val height: Double,
        val color: RaTeXColor,
    ) : DisplayItem()

    /**
     * Draws a vector path.
     *
     * Each command coordinate is relative to [x] and [y].
     *
     * @property x Horizontal path origin in font-size-relative units.
     * @property y Vertical path origin in font-size-relative units.
     * @property commands Path segments in drawing order.
     * @property fill Whether to fill the path; otherwise it is stroked.
     * @property color Fill or stroke color.
     */
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

/**
 * A vector-path segment used by [DisplayItem.Path].
 *
 * Coordinates are in font-size-relative units and are offset by the containing path's origin.
 */
@Serializable
sealed class PathCommand {
    /**
     * Starts a new contour at ([x], [y]).
     *
     * @property x Horizontal destination in font-size-relative units.
     * @property y Vertical destination in font-size-relative units.
     */
    @Serializable
    @SerialName("MoveTo")
    data class MoveTo(val x: Double, val y: Double) : PathCommand()

    /**
     * Adds a straight segment ending at ([x], [y]).
     *
     * @property x Horizontal destination in font-size-relative units.
     * @property y Vertical destination in font-size-relative units.
     */
    @Serializable
    @SerialName("LineTo")
    data class LineTo(val x: Double, val y: Double) : PathCommand()

    /**
     * Adds a cubic Bézier segment.
     *
     * All coordinates are in font-size-relative units.
     *
     * @property x1 Horizontal coordinate of the first control point.
     * @property y1 Vertical coordinate of the first control point.
     * @property x2 Horizontal coordinate of the second control point.
     * @property y2 Vertical coordinate of the second control point.
     * @property x Horizontal destination of the segment.
     * @property y Vertical destination of the segment.
     */
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

    /**
     * Adds a quadratic Bézier segment.
     *
     * All coordinates are in font-size-relative units.
     *
     * @property x1 Horizontal coordinate of the control point.
     * @property y1 Vertical coordinate of the control point.
     * @property x Horizontal destination of the segment.
     * @property y Vertical destination of the segment.
     */
    @Serializable
    @SerialName("QuadTo")
    data class QuadTo(
        val x1: Double,
        val y1: Double,
        val x: Double,
        val y: Double,
    ) : PathCommand()

    /** Closes the current path contour. */
    @Serializable
    @SerialName("Close")
    data object Close : PathCommand()
}

/**
 * An RGBA color stored in a display-list command.
 *
 * @property r Red channel, conventionally in the range `0.0..1.0`.
 * @property g Green channel, conventionally in the range `0.0..1.0`.
 * @property b Blue channel, conventionally in the range `0.0..1.0`.
 * @property a Alpha channel, conventionally in the range `0.0..1.0`.
 */
@Serializable
data class RaTeXColor(
    val r: Float,
    val g: Float,
    val b: Float,
    val a: Float,
) {
    /**
     * This color converted to a Compose [Color].
     *
     * Channel values outside `0.0..1.0` are clamped. The converted value is cached after its first
     * access.
     */
    val composeColor by lazy {
        Color(
            red = r.coerceIn(0f, 1f),
            green = g.coerceIn(0f, 1f),
            blue = b.coerceIn(0f, 1f),
            alpha = a.coerceIn(0f, 1f),
        )
    }
}

internal val ratexJson = Json {
    ignoreUnknownKeys = true
    classDiscriminator = "type"
}

/**
 * Pixel dimensions calculated for a [DisplayList].
 *
 * @property widthPx Horizontal advance in pixels.
 * @property heightPx Distance from the top to the baseline in pixels, including the renderer's
 * antialiasing guard.
 * @property depthPx Distance below the baseline in pixels, including the renderer's antialiasing
 * guard.
 */
data class RaTeXMeasuredDisplayList(
    val widthPx: Float,
    val heightPx: Float,
    val depthPx: Float,
) {
    /** Total pixel height above and below the baseline. */
    val totalHeightPx: Float get() = heightPx + depthPx
}

internal fun DisplayList.verticalAntialiasGuardPx(): Float =
    if (items.isEmpty()) 0f else 1f

/**
 * Scales this display list's metrics to [fontSizePx].
 *
 * This operation does not parse or draw the formula and is inexpensive enough to repeat when
 * density or font size changes. Callers using a Compose [androidx.compose.ui.unit.TextUnit] should
 * convert it with the current density before calling this function.
 *
 * @param fontSizePx Base font size in physical pixels.
 * @return Width, height, depth, and total height in pixels, including the same vertical
 * antialiasing guard used by the Compose renderer.
 */
fun DisplayList.measure(fontSizePx: Float): RaTeXMeasuredDisplayList = RaTeXMeasuredDisplayList(
    widthPx = (width * fontSizePx).toFloat(),
    heightPx = (height * fontSizePx).toFloat() + verticalAntialiasGuardPx(),
    depthPx = (depth * fontSizePx).toFloat() + verticalAntialiasGuardPx(),
)
