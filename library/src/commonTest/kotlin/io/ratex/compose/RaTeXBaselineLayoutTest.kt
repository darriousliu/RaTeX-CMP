package io.ratex.compose

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import io.ratex.DisplayItem
import io.ratex.DisplayList
import io.ratex.RaTeXColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

@OptIn(ExperimentalTestApi::class)
class RaTeXBaselineLayoutTest {
    @Test
    fun ratex_publishes_math_baseline_alignment_lines() = runComposeUiTest {
        var firstBaseline = AlignmentLine.Unspecified
        var lastBaseline = AlignmentLine.Unspecified
        var measuredWidth = -1
        var measuredHeight = -1

        setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 1f)) {
                Layout(
                    content = {
                        RaTeX(
                            displayList = baselineDisplayList(),
                            fontSize = 10.sp,
                        )
                    },
                ) { measurables, constraints ->
                    val placeable = measurables.single().measure(
                        constraints.copy(minWidth = 0, minHeight = 0)
                    )
                    firstBaseline = placeable[FirstBaseline]
                    lastBaseline = placeable[LastBaseline]
                    measuredWidth = placeable.width
                    measuredHeight = placeable.height
                    layout(placeable.width, placeable.height) {
                        placeable.placeRelative(0, 0)
                    }
                }
            }
        }

        waitForIdle()

        assertEquals(20, measuredWidth)
        assertEquals(13, measuredHeight)
        assertEquals(9, firstBaseline)
        assertEquals(9, lastBaseline)
    }

    @Test
    fun row_align_by_baseline_places_ratex_on_text_baseline() = runComposeUiTest {
        var beforeTextBaselineY = Float.NaN
        var formulaBaselineY = Float.NaN
        var afterTextBaselineY = Float.NaN

        setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 1f)) {
                Row {
                    Text(
                        text = "The value ",
                        fontSize = 10.sp,
                        modifier = Modifier
                            .alignByBaseline()
                            .onPlaced { beforeTextBaselineY = it.absoluteFirstBaselineY() },
                    )
                    RaTeX(
                        displayList = baselineDisplayList(),
                        fontSize = 10.sp,
                        modifier = Modifier
                            .alignByBaseline()
                            .onPlaced { formulaBaselineY = it.absoluteFirstBaselineY() },
                    )
                    Text(
                        text = " is important",
                        fontSize = 10.sp,
                        modifier = Modifier
                            .alignByBaseline()
                            .onPlaced { afterTextBaselineY = it.absoluteFirstBaselineY() },
                    )
                }
            }
        }

        waitForIdle()

        assertFalse(beforeTextBaselineY.isNaN())
        assertFalse(formulaBaselineY.isNaN())
        assertFalse(afterTextBaselineY.isNaN())
        assertEquals(beforeTextBaselineY, formulaBaselineY)
        assertEquals(beforeTextBaselineY, afterTextBaselineY)
    }

    private fun LayoutCoordinates.absoluteFirstBaselineY(): Float {
        val baseline = this[FirstBaseline]
        assertNotEquals(AlignmentLine.Unspecified, baseline)
        return positionInRoot().y + baseline
    }

    private fun baselineDisplayList(): DisplayList = DisplayList(
        width = 2.0,
        height = 0.8,
        depth = 0.3,
        items = listOf(
            DisplayItem.Line(
                x = 0.0,
                y = 0.8,
                width = 1.0,
                thickness = 0.05,
                color = RaTeXColor(0f, 0f, 0f, 1f),
            )
        ),
    )
}
