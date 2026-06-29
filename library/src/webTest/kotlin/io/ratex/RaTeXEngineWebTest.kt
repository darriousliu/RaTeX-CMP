package io.ratex

import kotlin.test.Test

class RaTeXEngineWebTest : RaTeXEngineCommonTestSuite() {
    @Test
    fun empty() {
    }

    override suspend fun beforeParse() {
        RaTeXEngine.initialize()
    }
}