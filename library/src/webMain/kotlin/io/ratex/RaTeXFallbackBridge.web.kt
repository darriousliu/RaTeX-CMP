@file:OptIn(ExperimentalWasmJsInterop::class)

package io.ratex

import androidx.compose.ui.graphics.Color
import io.ratex.compose.resources.Res
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise
import kotlin.js.js

@Serializable
private data class BridgeEntry(val codePoint: Int, val width: Double, val height: Double, val depth: Double)

@Serializable
private data class BridgeRequest(val latex: String, val displayMode: Boolean, val color: List<Float>, val metrics: List<BridgeEntry>)

private external interface MetricsWasm : JsAny {
    fun render(input: String): String
}

private external interface MetricsWasmBytes : JsAny {
    fun setByte(index: Int, value: Int)
}

@Suppress("UNUSED_PARAMETER")
private fun wasmBytes(size: Int): MetricsWasmBytes = js("""({
    buffer: new Uint8Array(size),
    setByte(index, value) { this.buffer[index] = value; }
})""")

@Suppress("UNUSED_PARAMETER")
private fun loadMetricsWasm(bytes: MetricsWasmBytes): Promise<MetricsWasm> = js("""WebAssembly.instantiate(bytes.buffer, {}).then(function(loaded) {
    const api = loaded.instance.exports;
    if (typeof api.ratex_cmp_bridge_version !== 'function' || api.ratex_cmp_bridge_version() !== 1) {
        throw new Error('Incompatible CMP metrics bridge');
    }
    const encoder = new TextEncoder();
    const decoder = new TextDecoder('utf-8', {fatal: true});
    function readString(pointer) {
        const bytes = new Uint8Array(api.memory.buffer);
        const end = bytes.indexOf(0, pointer);
        if (end < 0) throw new Error('Invalid bridge string');
        return decoder.decode(bytes.subarray(pointer, end));
    }
    return {render(input) {
        const bytes = encoder.encode(input);
        const pointer = api.ratex_cmp_alloc(bytes.length);
        if (!pointer) throw new Error('CMP metrics request exceeds the input limit');
        let result = 0;
        try {
            new Uint8Array(api.memory.buffer).set(bytes, pointer);
            result = api.ratex_cmp_parse_json(pointer, bytes.length);
            if (!result) {
                const error = api.ratex_cmp_last_error();
                throw new Error(error ? readString(error) : 'CMP metrics parse failed');
            }
            return readString(result);
        } finally {
            if (result) api.ratex_cmp_free(result);
            api.ratex_cmp_dealloc(pointer, bytes.length);
        }
    }};
})""")

internal actual object RaTeXFallbackBridge {
    private var runtime: MetricsWasm? = null
    private val initLock = Mutex()

    actual suspend fun prepare() {
        initLock.withLock {
            if (runtime != null) return
            try {
                // Use the resource reader so custom paths and pages such as /index.html work.
                val source = Res.readBytes("files/bridge/ratex_cmp_bridge.wasm")
                val bytes = wasmBytes(source.size)
                source.forEachIndexed { index, byte -> bytes.setByte(index, byte.toInt()) }
                runtime = loadMetricsWasm(bytes).await()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                throw RaTeXException("CMP metrics bridge initialization failed: ${error.message}")
            }
        }
    }

    actual fun parse(latex: String, displayMode: Boolean, color: Color, metrics: RaTeXFallbackMetrics): String {
        val engine = runtime ?: throw RaTeXException("Call prepareFallbackMetrics() before synchronous parsing on web targets")
        val request = BridgeRequest(latex, displayMode, listOf(color.red, color.green, color.blue, color.alpha),
            metrics.entries.map { (cp, value) -> BridgeEntry(cp, value.widthEm, value.heightEm, value.depthEm) })
        return try {
            engine.render(ratexJson.encodeToString(BridgeRequest.serializer(), request))
        } catch (error: Throwable) {
            throw RaTeXException(error.message ?: "CMP metrics parse failed")
        }
    }
}
