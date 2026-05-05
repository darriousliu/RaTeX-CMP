package io.ratex

import platform.Foundation.NSLock

private val systemFallbackCache = mutableMapOf<String, PlatformTypeFace>()
private val systemFallbackLock = NSLock()

internal actual fun resolvePlatformFallbackTypeFace(
    fontId: String,
    charCode: Int,
): PlatformTypeFace? {
    if (!isUnicodeFallbackFontId(fontId)) return null
    val cacheKey = "$fontId:$charCode"

    systemFallbackLock.lock()
    try {
        systemFallbackCache[cacheKey]?.let { return it }
    } finally {
        systemFallbackLock.unlock()
    }

    val typeFace = findSystemFallbackTypeFace(fontId, charCode) ?: return null

    systemFallbackLock.lock()
    try {
        systemFallbackCache[cacheKey] = typeFace
    } finally {
        systemFallbackLock.unlock()
    }
    return typeFace
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

        systemFallbackLock.lock()
        try {
            systemFallbackCache.clear()
        } finally {
            systemFallbackLock.unlock()
        }
    }
}
