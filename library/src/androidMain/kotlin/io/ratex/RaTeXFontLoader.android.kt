package io.ratex

import android.graphics.Typeface
import java.io.File
import java.util.concurrent.ConcurrentHashMap

internal actual typealias PlatformTypeFace = Typeface

internal actual fun decodePlatformTypeFace(fontId: String, bytes: ByteArray): PlatformTypeFace? {
    val tempDirectory = File(System.getProperty("java.io.tmpdir") ?: "/data/local/tmp").apply {
        mkdirs()
    }
    val fontFile = File.createTempFile(
        "ratex-${fontId.replace("[^A-Za-z0-9_.-]".toRegex(), "_")}-",
        ".ttf",
        tempDirectory,
    ).apply {
        writeBytes(bytes)
        deleteOnExit()
    }
    return Typeface.createFromFile(fontFile)
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
