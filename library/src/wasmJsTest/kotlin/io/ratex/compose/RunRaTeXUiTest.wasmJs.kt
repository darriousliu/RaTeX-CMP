package io.ratex.compose

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlinx.coroutines.test.TestResult

@OptIn(ExperimentalTestApi::class)
internal actual fun runRaTeXUiTest(block: suspend ComposeUiTest.() -> Unit): TestResult =
    runComposeUiTest(block = block)
