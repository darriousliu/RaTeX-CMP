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
