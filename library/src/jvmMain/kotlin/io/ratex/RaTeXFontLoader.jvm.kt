package io.ratex

import org.jetbrains.skia.Data
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.Typeface
import java.util.concurrent.ConcurrentHashMap

internal actual typealias PlatformTypeFace = Typeface

internal actual fun decodePlatformTypeFace(fontId: String, bytes: ByteArray): PlatformTypeFace? {
    val data = Data.makeFromBytes(bytes)
    return try {
        FontMgr.default.makeFromData(data)
    } finally {
        data.close()
    }
}

internal actual object FontCache {
    private val cache = ConcurrentHashMap<String, PlatformTypeFace>()

    actual operator fun get(fontId: String): PlatformTypeFace? {
        return cache[fontId]
    }

    actual operator fun set(fontId: String, typeFace: PlatformTypeFace) {
        cache[fontId] = typeFace
    }

    actual fun clear() {
        cache.clear()
    }
}
