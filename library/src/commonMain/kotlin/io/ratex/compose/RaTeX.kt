package io.ratex.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import io.ratex.DisplayList
import io.ratex.RaTeXEngine
import io.ratex.RaTeXFontLoader
import io.ratex.measure
import kotlin.math.ceil

/**
 * Remembers an asynchronous LaTeX parse operation for the current composition.
 *
 * The returned [State] is initially `null` while fonts are loaded and parsing is pending, then
 * contains either the parsed [DisplayList] or the failure. The result is retained at this call
 * position and parsing restarts whenever [latex], [displayMode], or [color] changes. This is a
 * composition-scoped cache; persist a successful display list elsewhere if it must outlive the
 * composition.
 *
 * Browser applications must initialize RaTeX WASM before calling this API. Errors are captured in
 * the returned [Result] instead of being thrown from composition.
 *
 * @param latex The LaTeX formula source.
 * @param displayMode `true` for display/block math style, or `false` for inline/text math style.
 * @param color The color embedded in the parsed drawing commands. Defaults to the current
 * [LocalContentColor].
 * @return Observable parse state: `null` while pending, then a success or failure result.
 */
@Composable
fun rememberRaTeXDisplayList(
    latex: String,
    displayMode: Boolean = true,
    color: Color = LocalContentColor.current,
): State<Result<DisplayList>?> = produceState(initialValue = null, latex, displayMode, color) {
    value = runCatching {
        RaTeXFontLoader.ensureLoaded()
        RaTeXEngine.parse(latex, displayMode, color)
    }
}

/**
 * Remembers a synchronous LaTeX parse result for the current composition.
 *
 * Font loading and parsing block the current composition the first time this call is evaluated for
 * a set of inputs. The [Result] is reused until [latex], [displayMode], or [color] changes. This
 * helper is commonly used with [androidx.compose.material3.Text]'s `inlineContent`, where constructing `InlineTextContent`
 * requires immediate formula metrics to size its placeholder. Prefer [rememberRaTeXDisplayList]
 * for ordinary rendering.
 *
 * Browser targets cannot initialize Compose resources or RaTeX WASM synchronously, so this helper
 * is usable there only after asynchronous initialization has completed. Parsing and font-loading
 * failures are captured in the returned [Result].
 *
 * @param latex The LaTeX formula source.
 * @param displayMode `true` for display/block math style, or `false` for inline/text math style.
 * @param color The color embedded in the parsed drawing commands. Defaults to the current
 * [LocalContentColor].
 * @return A cached success containing the display list, or a failure describing why it could not
 * be produced.
 */
@Composable
fun rememberBlockingRaTeXDisplayList(
    latex: String,
    displayMode: Boolean = true,
    color: Color = LocalContentColor.current,
): Result<DisplayList?> {
    return remember(latex, displayMode, color) {
        runCatching {
            ensureRaTeXFontsLoadedBlocking()
            RaTeXEngine.parseBlocking(latex, displayMode, color)
        }
    }
}

/**
 * Parses and renders a LaTeX formula.
 *
 * Font loading and parsing run asynchronously. Until parsing succeeds, or if parsing fails, this
 * overload emits empty content. Use [rememberRaTeXDisplayList] with the [RaTeX] overload that takes
 * a [DisplayList] when the UI needs to display loading or error state, or when one parse result
 * should be reused in multiple places.
 *
 * @param latex The LaTeX formula source to render.
 * @param modifier Modifier applied to the formula layout.
 * @param fontSize Base rendering size. Changing it rescales the display list without changing the
 * formula's math style.
 * @param displayMode `true` for display/block math style, or `false` for inline/text math style.
 * This controls TeX layout; it does not itself place the composable in a block or text line.
 * @param color Formula color. Defaults to the current [LocalContentColor].
 */
@Composable
fun RaTeX(
    latex: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 28.sp,
    displayMode: Boolean = true,
    color: Color = LocalContentColor.current,
) {
    val parseResult by rememberRaTeXDisplayList(
        latex = latex,
        displayMode = displayMode,
        color = color,
    )
    RaTeX(
        displayList = parseResult?.getOrNull(),
        modifier = modifier,
        fontSize = fontSize,
    )
}

/**
 * Renders an already parsed [DisplayList].
 *
 * This overload performs no LaTeX parsing, so the same display list can be cached and reused
 * across multiple compositions or rendered at different [fontSize] values. Display mode and color
 * are part of the parsed commands and can only be changed by parsing a new list. Fonts are loaded
 * asynchronously; [displayList] equal to `null` emits empty content.
 *
 * @param displayList Parsed drawing commands, or `null` to render an empty formula.
 * @param modifier Modifier applied to the formula layout.
 * @param fontSize Base rendering size used to scale the display-list metrics and commands.
 */
@Composable
fun RaTeX(
    displayList: DisplayList?,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 28.sp,
) {
    val density = LocalDensity.current
    val fontSizePx = with(density) { fontSize.toPx() }
    val measuredDisplayList = remember(displayList, fontSizePx) {
        displayList?.measure(fontSizePx)
    }
    var fontsReady by remember(displayList) { mutableStateOf(false) }

    LaunchedEffect(displayList) {
        fontsReady = runCatching {
            RaTeXFontLoader.ensureLoaded()
            true
        }.getOrDefault(false)
    }

    Layout(
        modifier = modifier,
        content = {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val currentDisplayList = displayList ?: return@Canvas
                drawDisplayList(
                    displayList = currentDisplayList,
                    fontSizePx = fontSizePx,
                    drawGlyph = { glyph, glyphFontSizePx ->
                        if (fontsReady) {
                            drawPlatformGlyph(glyph, glyphFontSizePx)
                        }
                    },
                )
            }
        },
    ) { measurables, constraints ->
        val desiredWidth = measuredDisplayList?.widthPx?.ceilPx()?.toInt() ?: 0
        val desiredHeight = measuredDisplayList?.totalHeightPx?.ceilPx()?.toInt() ?: 0
        val width = desiredWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = desiredHeight.coerceIn(constraints.minHeight, constraints.maxHeight)
        val placeable = measurables.single().measure(
            constraints.copy(
                minWidth = width,
                maxWidth = width,
                minHeight = height,
                maxHeight = height,
            )
        )
        val alignmentLines: Map<AlignmentLine, Int> = measuredDisplayList?.let {
            val baseline = it.heightPx.ceilPx().toInt().coerceIn(0, height)
            mapOf(
                FirstBaseline to baseline,
                LastBaseline to baseline,
            )
        } ?: emptyMap()

        layout(width, height, alignmentLines) {
            placeable.placeRelative(0, 0)
        }
    }
}

private fun Float.ceilPx(): Float = ceil(toDouble()).toFloat()
