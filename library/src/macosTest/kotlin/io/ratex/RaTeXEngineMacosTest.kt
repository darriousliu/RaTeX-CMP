package io.ratex

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class RaTeXEngineMacosTest : RaTeXEngineCommonTestSuite() {
    @Test
    fun empty() {
    }

    @Test
    fun bundled_fonts_load_on_macos() = runBlocking {
        RaTeXFontLoader.clear()
        assertEquals(19, RaTeXFontLoader.loadFromResources())
    }
}
