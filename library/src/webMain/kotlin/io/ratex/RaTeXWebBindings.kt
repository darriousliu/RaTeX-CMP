@file:OptIn(ExperimentalWasmJsInterop::class)

package io.ratex

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsModule
import kotlin.js.Promise
import kotlin.js.js

@JsModule("ratex-wasm")
private external object RaTeXWasmModule {
    fun initRatex(): Promise<JsAny?>

    fun renderLatex(latex: String, color: String): String
}

@Suppress("UNUSED_PARAMETER")
private fun renderLatexCatching(
    renderLatex: (String, String) -> String,
    latex: String,
    color: String,
): String = js(
    """{
        try {
            return renderLatex(latex, color);
        } catch (error) {
            throw new Error(String(error));
        }
    }"""
)

internal fun initRatex(): Promise<JsAny?> =
    RaTeXWasmModule.initRatex()

internal fun renderLatex(latex: String, color: String): String =
    renderLatexCatching(
        renderLatex = { source, cssColor -> RaTeXWasmModule.renderLatex(source, cssColor) },
        latex = latex,
        color = color,
    )
