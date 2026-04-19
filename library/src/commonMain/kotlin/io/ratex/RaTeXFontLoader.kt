package io.ratex

import io.ratex.compose.resources.Res
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.jvm.JvmStatic

object RaTeXFontLoader {
    private val fontsLoaded = atomic(false)
    private val loadLock = Mutex()

    /** KaTeX font IDs (Rust FontId.as_str()) → TTF filename without path. */
    private val fontFileNames = listOf(
        "AMS-Regular" to "KaTeX_AMS-Regular.ttf",
        "Caligraphic-Regular" to "KaTeX_Caligraphic-Regular.ttf",
        "Fraktur-Regular" to "KaTeX_Fraktur-Regular.ttf",
        "Fraktur-Bold" to "KaTeX_Fraktur-Bold.ttf",
        "Main-Bold" to "KaTeX_Main-Bold.ttf",
        "Main-BoldItalic" to "KaTeX_Main-BoldItalic.ttf",
        "Main-Italic" to "KaTeX_Main-Italic.ttf",
        "Main-Regular" to "KaTeX_Main-Regular.ttf",
        "Math-BoldItalic" to "KaTeX_Math-BoldItalic.ttf",
        "Math-Italic" to "KaTeX_Math-Italic.ttf",
        "SansSerif-Bold" to "KaTeX_SansSerif-Bold.ttf",
        "SansSerif-Italic" to "KaTeX_SansSerif-Italic.ttf",
        "SansSerif-Regular" to "KaTeX_SansSerif-Regular.ttf",
        "Script-Regular" to "KaTeX_Script-Regular.ttf",
        "Size1-Regular" to "KaTeX_Size1-Regular.ttf",
        "Size2-Regular" to "KaTeX_Size2-Regular.ttf",
        "Size3-Regular" to "KaTeX_Size3-Regular.ttf",
        "Size4-Regular" to "KaTeX_Size4-Regular.ttf",
        "Typewriter-Regular" to "KaTeX_Typewriter-Regular.ttf",
    )

    /**
     * Ensure KaTeX fonts are loaded; if not, load from resources. No-op if already loaded.
     * @return Number of fonts loaded (0 if already loaded)
     */
    suspend fun ensureLoaded(): Int {
        if (fontsLoaded.value) return 0
        loadLock.withLock {
            if (fontsLoaded.value) return 0
            return loadFromResources().also { fontsLoaded.value = true }
        }
    }

    /**
     * Load KaTeX fonts from Compose resources.
     * @return Number of fonts successfully loaded
     */
    suspend fun loadFromResources(): Int {
        FontCache.clear()
        var loadedFonts = 0
        fontFileNames.forEach { (fontId, fileName) ->
            runCatching {
                Res.readBytes("files/fonts/$fileName")
            }.onSuccess { bytes ->
                val typeFace = decodePlatformTypeFace(fontId, bytes)
                if (typeFace != null) {
                    FontCache[fontId] = typeFace
                    loadedFonts++
                } else {
                    println("Failed to decode Compose font resource '$fileName' for '$fontId'")
                }
            }.onFailure {
                println("Failed to load Compose font resource '$fileName' for '$fontId'")
            }
        }
        return loadedFonts
    }

    @JvmStatic
    internal fun getPlatformTypeFace(fontId: String): PlatformTypeFace? = FontCache[fontId]

    @JvmStatic
    fun clear() {
        FontCache.clear()
        fontsLoaded.value = false
    }
}

internal expect class PlatformTypeFace

internal expect fun decodePlatformTypeFace(fontId: String, bytes: ByteArray): PlatformTypeFace?

internal expect object FontCache {
    operator fun get(fontId: String): PlatformTypeFace?
    operator fun set(fontId: String, typeFace: PlatformTypeFace)
    fun clear()
}
