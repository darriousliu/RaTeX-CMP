package io.ratex.compose

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlinx.coroutines.test.TestResult
import kotlin.js.Promise
import kotlin.js.unsafeCast

// Load Skiko's generated JS exports as well as its WASM before creating a surface.
@JsModule("./js-reexport-symbols.mjs")
private external object SkikoTestRuntime {
    val api: SkikoTestApi
}

private external interface SkikoTestApi {
    val awaitSkiko: Promise<Any?>
}

@OptIn(ExperimentalTestApi::class)
internal actual fun runRaTeXUiTest(block: suspend ComposeUiTest.() -> Unit): TestResult =
    // JavaScript Promise resolution flattens the test's returned Promise.
    SkikoTestRuntime.api.awaitSkiko.then { runComposeUiTest(block = block) }.unsafeCast<TestResult>()
