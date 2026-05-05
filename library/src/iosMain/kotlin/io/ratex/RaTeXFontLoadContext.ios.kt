package io.ratex

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlin.coroutines.CoroutineContext

internal actual val ratexFontLoadContext: CoroutineContext = Dispatchers.IO
