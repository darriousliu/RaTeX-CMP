package io.ratex.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import io.ratex.DisplayList
import io.ratex.RaTeXEngine
import io.ratex.RaTeXFontLoader
import io.ratex.measure

@Composable
fun rememberRaTeXDisplayList(
    latex: String,
    displayMode: Boolean = true,
): State<Result<DisplayList>?> = produceState(initialValue = null, latex, displayMode) {
    value = runCatching {
        RaTeXFontLoader.ensureLoaded()
        RaTeXEngine.parse(latex, displayMode)
    }
}

@Composable
fun RaTeX(
    latex: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 28.sp,
    displayMode: Boolean = true,
) {
    val parseResult by rememberRaTeXDisplayList(
        latex = latex,
        displayMode = displayMode,
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
    val width = with(density) { (measuredDisplayList?.widthPx ?: 0f).toDp() }
    val height = with(density) { (measuredDisplayList?.totalHeightPx ?: 0f).toDp() }
    var fontsReady by remember(displayList) { mutableStateOf(false) }

    LaunchedEffect(displayList) {
        fontsReady = runCatching {
            RaTeXFontLoader.ensureLoaded()
            true
        }.getOrDefault(false)
    }

    Canvas(
        modifier = modifier.size(width, height),
    ) {
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
}
