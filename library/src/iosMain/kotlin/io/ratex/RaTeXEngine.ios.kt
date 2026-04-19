@file:OptIn(ExperimentalForeignApi::class)

package io.ratex

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.toKString
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ratex.RatexOptions
import ratex.ratex_free_display_list
import ratex.ratex_get_last_error
import ratex.ratex_parse_and_layout

internal actual object RaTeXEngine {
    actual suspend fun parse(latex: String, displayMode: Boolean): DisplayList =
        withContext(Dispatchers.Default) { parseBlocking(latex, displayMode) }

    actual fun parseBlocking(latex: String, displayMode: Boolean): DisplayList {
        val json = parseNativeDisplayListJson(latex, displayMode)
            ?: throw RaTeXException(nativeLastErrorMessage() ?: "unknown error")
        return try {
            ratexJson.decodeFromString(DisplayList.serializer(), json)
        } catch (error: Exception) {
            throw RaTeXException("JSON decode failed: ${error.message}")
        }
    }
}

private fun parseNativeDisplayListJson(
    latex: String,
    displayMode: Boolean,
): String? = memScoped {
    val options = alloc<RatexOptions>()
    options.struct_size = sizeOf<RatexOptions>().convert()
    options.display_mode = if (displayMode) 1 else 0

    val result = ratex_parse_and_layout(latex, options.ptr)
    result.useContents {
        if (error_code != 0 || data == null) {
            null
        } else {
            val json = data?.toKString()
            ratex_free_display_list(data)
            json
        }
    }
}

private fun nativeLastErrorMessage(): String? =
    ratex_get_last_error()?.toKString()
