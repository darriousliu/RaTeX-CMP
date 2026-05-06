package io.ratex

import platform.Foundation.NSLock

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
