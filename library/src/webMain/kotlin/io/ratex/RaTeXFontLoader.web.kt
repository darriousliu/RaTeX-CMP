package io.ratex

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
    }
}
