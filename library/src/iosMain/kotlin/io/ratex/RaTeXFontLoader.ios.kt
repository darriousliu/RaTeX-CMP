package io.ratex

import org.jetbrains.skia.Data
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Typeface
import platform.Foundation.NSLock

internal actual typealias PlatformTypeFace = Typeface

private val systemFallbackCache = mutableMapOf<String, PlatformTypeFace>()
private val systemFallbackLock = NSLock()

internal actual fun decodePlatformTypeFace(fontId: String, bytes: ByteArray): PlatformTypeFace? {
    val data = Data.makeFromBytes(bytes)
    return try {
        FontMgr.default.makeFromData(data)
    } finally {
        data.close()
    }
}

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

private fun findSystemFallbackTypeFace(
    fontId: String,
    charCode: Int,
): PlatformTypeFace? {
    val fontMgr = FontMgr.default
    val families = fallbackFamilies(fontId)
    val locales = fallbackLocales(fontId)

    runCatching {
        fontMgr.matchFamiliesStyleCharacter(
            families,
            FontStyle.NORMAL,
            locales,
            charCode,
        )
    }.getOrNull()?.takeIf { it.supports(charCode) }?.let { return it }

    families.forEach { family ->
        runCatching {
            fontMgr.matchFamilyStyleCharacter(
                family,
                FontStyle.NORMAL,
                locales,
                charCode,
            )
        }.getOrNull()?.takeIf { it.supports(charCode) }?.let { return it }
    }

    families.forEach { family ->
        runCatching {
            fontMgr.matchFamilyStyle(family, FontStyle.NORMAL)
        }.getOrNull()?.takeIf { it.supports(charCode) }?.let { return it }
    }

    return runCatching {
        fontMgr.legacyMakeTypeface("sans-serif", FontStyle.NORMAL)
    }.getOrNull()?.takeIf { it.supports(charCode) }
}

private fun fallbackFamilies(fontId: String): Array<String?> = when (fontId) {
    FONT_ID_EMOJI_FALLBACK -> arrayOf(
        "Apple Color Emoji",
        "Noto Color Emoji",
        "Segoe UI Emoji",
        "Noto Emoji",
        "Twemoji Mozilla",
        "sans-serif",
    )
    FONT_ID_CJK_FALLBACK -> arrayOf(
        "Noto Sans Symbols 2",
        "Noto Sans Symbols",
        "Apple Symbols",
        "Segoe UI Symbol",
        "Arial Unicode MS",
        "sans-serif",
    )
    else -> arrayOf(
        "PingFang SC",
        "Hiragino Sans GB",
        "Noto Sans CJK SC",
        "Noto Sans CJK JP",
        "Noto Sans CJK KR",
        "Microsoft YaHei",
        "SimHei",
        "Arial Unicode MS",
        "Apple Color Emoji",
        "Noto Color Emoji",
        "Segoe UI Emoji",
        "Noto Sans Symbols 2",
        "Noto Sans Symbols",
        "Apple Symbols",
        "Segoe UI Symbol",
        "sans-serif",
    )
}

private fun fallbackLocales(fontId: String): Array<String> = when (fontId) {
    FONT_ID_EMOJI_FALLBACK -> arrayOf("und-Zsye", "en")
    FONT_ID_CJK_FALLBACK -> arrayOf("en", "zh-Hans")
    else -> arrayOf("zh-Hans", "zh-Hant", "ja", "ko", "en")
}

private fun PlatformTypeFace.supports(charCode: Int): Boolean =
    runCatching { getUTF32Glyph(charCode).toInt() != 0 }.getOrDefault(false)

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
