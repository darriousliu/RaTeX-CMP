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

    fun renderLatex(latex: String, color: String, displayMode: Boolean): String
}

@Suppress("UNUSED_PARAMETER")
private fun renderLatexCatching(
    renderLatex: (String, String, Boolean) -> String,
    latex: String,
    color: String,
    displayMode: Boolean,
): String = js(
    """{
        try {
            return renderLatex(latex, color, displayMode);
        } catch (error) {
            throw new Error(String(error));
        }
    }"""
)

internal fun initRatex(): Promise<JsAny?> =
    RaTeXWasmModule.initRatex()

internal fun renderLatex(latex: String, color: String, displayMode: Boolean): String =
    renderLatexCatching(
        renderLatex = { source, cssColor, isDisplayMode ->
            RaTeXWasmModule.renderLatex(source, cssColor, isDisplayMode)
        },
        latex = latex,
        color = color,
        displayMode = displayMode,
    )
