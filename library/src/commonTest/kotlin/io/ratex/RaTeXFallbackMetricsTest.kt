package io.ratex

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class RaTeXFallbackMetricsTest {
    @Test
    fun input_is_copied_and_empty_map_is_shared() {
        val original = FallbackGlyphMetrics(0.82, 0.53, 0.12)
        val mutable = mutableMapOf('ш'.code to original)
        val snapshot = RaTeXFallbackMetrics.of(mutable)
        mutable['ш'.code] = FallbackGlyphMetrics(2.0, 1.0, 0.0)
        mutable.clear()
        assertEquals(original, snapshot.entries['ш'.code])
        assertSame(RaTeXFallbackMetrics.Empty, RaTeXFallbackMetrics.of(emptyMap()))
    }

    @Test
    fun rejects_invalid_scalars_counts_and_dimensions() {
        val valid = FallbackGlyphMetrics(0.0, 0.0, 0.0)
        for (cp in listOf(-1, 0xD800, 0xDFFF, 0x110000)) {
            assertFailsWith<IllegalArgumentException> { RaTeXFallbackMetrics.of(mapOf(cp to valid)) }
        }
        assertEquals(1, RaTeXFallbackMetrics.of(mapOf(0x1F60A to valid)).size)
        assertFailsWith<IllegalArgumentException> { RaTeXFallbackMetrics.of((0..4096).associateWith { valid }) }
        for (value in listOf(Double.NaN, Double.POSITIVE_INFINITY, -0.01, 100.01)) {
            assertFailsWith<IllegalArgumentException> { FallbackGlyphMetrics(value, 0.0, 0.0) }
            assertFailsWith<IllegalArgumentException> { FallbackGlyphMetrics(0.0, value, 0.0) }
            assertFailsWith<IllegalArgumentException> { FallbackGlyphMetrics(0.0, 0.0, value) }
        }
    }
}
