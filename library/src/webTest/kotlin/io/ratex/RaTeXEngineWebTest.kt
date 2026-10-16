package io.ratex

class RaTeXEngineWebTest : RaTeXEngineCommonTestSuite() {
    override suspend fun beforeParse() {
        RaTeXEngine.initialize()
    }
}
