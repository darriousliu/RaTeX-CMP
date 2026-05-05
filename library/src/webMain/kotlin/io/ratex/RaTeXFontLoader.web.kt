package io.ratex

private val systemFallbackCache = mutableMapOf<String, PlatformTypeFace>()

internal actual fun resolvePlatformFallbackTypeFace(
    fontId: String,
    charCode: Int,
): PlatformTypeFace? {
    if (!isUnicodeFallbackFontId(fontId)) return null
    val cacheKey = "$fontId:$charCode"
    systemFallbackCache[cacheKey]?.let { return it }

    val typeFace = findSystemFallbackTypeFace(fontId, charCode) ?: return null
    systemFallbackCache[cacheKey] = typeFace
    return typeFace
}

internal actual object FontCache {
    private val cache = mutableMapOf<String, PlatformTypeFace>()

    actual operator fun get(fontId: String): PlatformTypeFace? {
        return cache[fontId]
    }

    actual operator fun set(fontId: String, typeFace: PlatformTypeFace) {
        cache[fontId] = typeFace
    }

    actual fun clear() {
        cache.clear()
        systemFallbackCache.clear()
    }
}
