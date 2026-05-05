package io.ratex

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

internal actual val ratexFontLoadContext: CoroutineContext = Dispatchers.IO
