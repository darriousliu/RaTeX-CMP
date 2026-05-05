package io.ratex

internal fun Int.toCodePointString(): String? {
    if (this !in 0..0x10FFFF || this in 0xD800..0xDFFF) return null
    if (this <= 0xFFFF) return toChar().toString()

    val surrogate = this - 0x10000
    val high = ((surrogate ushr 10) + 0xD800).toChar()
    val low = ((surrogate and 0x3FF) + 0xDC00).toChar()
    return charArrayOf(high, low).concatToString()
}
