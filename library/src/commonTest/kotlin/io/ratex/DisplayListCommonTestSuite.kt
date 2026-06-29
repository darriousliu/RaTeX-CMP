package io.ratex

import kotlin.test.Test
import kotlin.test.assertEquals

abstract class DisplayListCommonTestSuite {
    @Test
    fun measure_adds_vertical_guard_for_non_empty_display_lists() {
        val displayList = DisplayList(
            width = 2.0,
            height = 3.0,
            depth = 0.5,
            items = listOf(
                DisplayItem.GlyphPath(
                    x = 0.0,
                    y = 0.0,
                    scale = 1.0,
                    font = "KaTeX_Main-Regular",
                    charCode = 'x'.code,
                    color = RaTeXColor(0f, 0f, 0f, 1f),
                ),
            ),
        )

        val measured = displayList.measure(fontSizePx = 10f)

        assertEquals(20f, measured.widthPx)
        assertEquals(31f, measured.heightPx)
        assertEquals(6f, measured.depthPx)
        assertEquals(37f, measured.totalHeightPx)
    }

    @Test
    fun measure_keeps_empty_display_lists_tight() {
        val displayList = DisplayList(
            width = 0.0,
            height = 0.0,
            depth = 0.0,
            items = emptyList(),
        )

        val measured = displayList.measure(fontSizePx = 10f)

        assertEquals(0f, measured.widthPx)
        assertEquals(0f, measured.heightPx)
        assertEquals(0f, measured.depthPx)
        assertEquals(0f, measured.totalHeightPx)
    }
}
