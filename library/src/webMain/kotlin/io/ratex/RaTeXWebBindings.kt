@file:OptIn(ExperimentalWasmJsInterop::class)

package io.ratex

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsModule
import kotlin.js.Promise

@JsModule("ratex-wasm")
private external object RaTeXWasmModule {
    fun initRatex(): Promise<JsAny?>

    fun renderLatex(latex: String, color: String): String
}

internal fun initRatex(): Promise<JsAny?> =
    RaTeXWasmModule.initRatex()

internal fun renderLatex(latex: String, color: String): String =
    RaTeXWasmModule.renderLatex(latex, color)
