package io.ratex

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

private const val MAX_SYSTEM_FALLBACKS_PER_FONT_ID = 8

internal actual object FontCache {
    private val cache = ConcurrentHashMap<String, PlatformTypeFace>()
    private val systemFallbacks = ConcurrentHashMap<String, CopyOnWriteArrayList<PlatformTypeFace>>()

    actual operator fun get(fontId: String): PlatformTypeFace? {
        return cache[fontId]
    }

    actual operator fun set(fontId: String, typeFace: PlatformTypeFace) {
        cache[fontId] = typeFace
    }

    actual fun getSystemFallback(fontId: String, charCode: Int): PlatformTypeFace? {
        return systemFallbacks[fontId]?.firstOrNull { typeFace ->
            platformTypeFaceSupports(typeFace, charCode)
        }
    }

    actual fun addSystemFallback(fontId: String, typeFace: PlatformTypeFace) {
        val fallbacks = systemFallbacks.computeIfAbsent(fontId) { CopyOnWriteArrayList() }
        if (typeFace in fallbacks) return
        synchronized(fallbacks) {
            if (typeFace in fallbacks) return
            if (fallbacks.size >= MAX_SYSTEM_FALLBACKS_PER_FONT_ID) {
                fallbacks.removeAt(0)
            }
            fallbacks.add(typeFace)
        }
    }

    actual fun clear() {
        cache.clear()
        systemFallbacks.clear()
    }
}
