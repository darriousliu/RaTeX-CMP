package io.ratex

import kotlinx.coroutines.await
import kotlin.js.Promise

@OptIn(ExperimentalWasmJsInterop::class)
internal actual suspend fun <T : JsAny?> Promise<T>.await(): T = await()