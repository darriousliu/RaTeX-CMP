package io.ratex.compose

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import kotlinx.coroutines.test.TestResult

/** Initializes the platform renderer before the UI test creates its drawing surface. */
@OptIn(ExperimentalTestApi::class)
internal expect fun runRaTeXUiTest(block: suspend ComposeUiTest.() -> Unit): TestResult
