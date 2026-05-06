package io.ratex

import platform.Foundation.NSLock

private const val MAX_SYSTEM_FALLBACKS_PER_FONT_ID = 8

internal actual object FontCache {
    private val cache = mutableMapOf<String, PlatformTypeFace>()
    private val systemFallbacks = mutableMapOf<String, MutableList<PlatformTypeFace>>()
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

    actual fun getSystemFallback(fontId: String, charCode: Int): PlatformTypeFace? {
        lock.lock()
        return try {
            systemFallbacks[fontId]?.firstOrNull { typeFace ->
                platformTypeFaceSupports(typeFace, charCode)
            }
        } finally {
            lock.unlock()
        }
    }

    actual fun addSystemFallback(fontId: String, typeFace: PlatformTypeFace) {
        lock.lock()
        try {
            val fallbacks = systemFallbacks.getOrPut(fontId) { mutableListOf() }
            if (typeFace in fallbacks) return
            if (fallbacks.size >= MAX_SYSTEM_FALLBACKS_PER_FONT_ID) {
                fallbacks.removeAt(0)
            }
            fallbacks.add(typeFace)
        } finally {
            lock.unlock()
        }
    }

    actual fun clear() {
        lock.lock()
        try {
            cache.clear()
            systemFallbacks.clear()
        } finally {
            lock.unlock()
        }
    }
}
