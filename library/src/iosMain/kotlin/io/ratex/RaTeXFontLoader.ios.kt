package io.ratex

import org.jetbrains.skia.Data
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.Typeface
import platform.Foundation.NSLock

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
    private val cache = mutableMapOf<String, PlatformTypeFace>()
    private val lock = NSLock()

    actual operator fun get(fontId: String): PlatformTypeFace? {
        lock.lock()
        return try {
            cache[fontId]
        } finally {
            lock.unlock()
        }
    }

    actual operator fun set(fontId: String, typeFace: PlatformTypeFace) {
        lock.lock()
        try {
            cache[fontId] = typeFace
        } finally {
            lock.unlock()
        }
    }

    actual fun clear() {
        lock.lock()
        try {
            cache.clear()
        } finally {
            lock.unlock()
        }
    }
}
