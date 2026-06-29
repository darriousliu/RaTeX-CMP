package io.ratex

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RaTeXEngineJvmTest : RaTeXEngineCommonTestSuite() {
    @Test
    fun parse_0_1_11_reports_recursion_limit() {
        val latex = "{".repeat(600) + "x" + "}".repeat(600)

        val error = assertFailsWith<RaTeXException> {
            RaTeXEngine.parseBlocking(latex, displayMode = true)
        }
        assertEquals(
            error.message?.contains("Recursion limit exceeded"),
            true,
            "Expected recursion limit error, got: ${error.message}"
        )
    }
}
