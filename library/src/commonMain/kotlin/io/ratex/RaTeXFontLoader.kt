package io.ratex

import io.ratex.RaTeXFontLoader.clear
import io.ratex.RaTeXFontLoader.ensureLoaded
import io.ratex.RaTeXFontLoader.loadFromResources
import io.ratex.compose.resources.Res
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmStatic

/**
 * Manages the process-wide font cache used by the Compose RaTeX renderer.
 *
 * Bundled KaTeX fonts are loaded lazily by [ensureLoaded]. Applications can register additional
 * CJK or emoji fonts after bundled loading completes. Registered fonts remain cached until [clear]
 * or [loadFromResources] is called.
 */
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
     * Ensures that bundled KaTeX fonts are loaded.
     *
     * Concurrent callers are serialized, and loading work runs on the platform font-loading
     * context instead of the caller's context. This operation is process-wide and idempotent:
     * after the first completed load it returns `0`. Individual resource or decoding failures are
     * logged and skipped; system Unicode fallback fonts are resolved lazily while drawing.
     *
     * Call this before registering app-provided fonts because the initial bundled load clears the
     * font cache.
     *
     * @return The number of bundled fonts loaded, or `0` if loading had already completed.
     */
    suspend fun ensureLoaded(): Int {
        if (fontsLoaded.value) return 0
        loadLock.withLock {
            if (fontsLoaded.value) return 0
            return withContext(ratexFontLoadContext) {
                loadFromResources().also {
                    fontsLoaded.value = true
                }
            }
        }
    }

    /**
     * Clears the current font cache and immediately loads bundled KaTeX fonts from Compose
     * resources.
     *
     * This is the lower-level, non-idempotent loader. It is not protected by [ensureLoaded]'s
     * mutex and does not update its loaded flag, so most callers should use [ensureLoaded].
     * App-provided and lazily resolved fallback fonts are removed when loading starts. Individual
     * resource or decoding failures are logged and skipped.
     *
     * @return The number of bundled fonts successfully loaded.
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

    /**
     * Registers an app-provided font for a RaTeX font ID.
     *
     * Common fallback IDs include `CJK-Regular`, `CJK-Fallback`, and `Emoji-Fallback`. Browser
     * targets do not have reliable access to host system fonts from Skia, so applications that
     * render CJK or emoji should register suitable fonts after [ensureLoaded]. Registration
     * mutates the process-wide cache; perform it during application initialization rather than
     * concurrently with rendering.
     *
     * @param fontId RaTeX font ID that should resolve to this typeface.
     * @param bytes Complete bytes of a font supported by the current platform.
     * @return `true` when the font was decoded and cached, or `false` when decoding is unsupported
     * or fails without an exception.
     */
    @JvmStatic
    fun registerFont(
        fontId: String,
        bytes: ByteArray,
    ): Boolean {
        val typeFace = decodePlatformTypeFace(fontId, bytes) ?: return false
        FontCache[fontId] = typeFace
        return true
    }

    /**
     * Registers one app-provided CJK font as both `CJK-Regular` and `CJK-Fallback`.
     *
     * Call this after [ensureLoaded]. Registration mutates the process-wide cache and should
     * normally happen during application initialization.
     *
     * @param bytes Complete bytes of a CJK font supported by the current platform.
     * @return `2` when the typeface was decoded and registered under both IDs, or `0` when
     * decoding is unsupported or fails without an exception.
     */
    @JvmStatic
    fun registerCjkFallbackFont(bytes: ByteArray): Int {
        val typeFace = decodePlatformTypeFace(FONT_ID_CJK_REGULAR, bytes) ?: return 0
        FontCache[FONT_ID_CJK_REGULAR] = typeFace
        FontCache[FONT_ID_CJK_FALLBACK] = typeFace
        return 2
    }

    /**
     * Registers an app-provided font as `Emoji-Fallback`.
     *
     * Call this after [ensureLoaded], especially on browser targets where a suitable system emoji
     * typeface may not be available to Skia.
     *
     * @param bytes Complete bytes of an emoji font supported by the current platform.
     * @return `true` when the font was decoded and cached, or `false` when decoding is unsupported
     * or fails without an exception.
     */
    @JvmStatic
    fun registerEmojiFallbackFont(bytes: ByteArray): Boolean =
        registerFont(FONT_ID_EMOJI_FALLBACK, bytes)

    @JvmStatic
    internal fun getPlatformTypeFace(
        fontId: String,
        charCode: Int? = null,
    ): PlatformTypeFace? {
        if (!isUnicodeFallbackFontId(fontId)) return FontCache[fontId]

        val codePoint = fallbackCodePoint(fontId, charCode)
        FontCache[fontId]?.takeIf { typeFace ->
            platformTypeFaceSupports(typeFace, codePoint)
        }?.let { return it }

        registeredFallbackFontIds(fontId).forEach { fallbackFontId ->
            FontCache[fallbackFontId]?.takeIf { typeFace ->
                platformTypeFaceSupports(typeFace, codePoint)
            }?.let { return it }
        }

        FontCache.getSystemFallback(fontId, codePoint)?.let { return it }

        return resolvePlatformFallbackTypeFace(
            fontId = fontId,
            charCode = codePoint,
        )?.also { typeFace ->
            FontCache.addSystemFallback(fontId, typeFace)
        }
    }

    /**
     * Clears all bundled, app-provided, and lazily resolved fonts from the process-wide cache.
     *
     * The next [ensureLoaded] call reloads bundled fonts. Avoid clearing the cache concurrently
     * with rendering; this function is primarily useful for tests or explicit font
     * reconfiguration.
     */
    @JvmStatic
    fun clear() {
        FontCache.clear()
        fontsLoaded.value = false
    }
}

internal expect val ratexFontLoadContext: CoroutineContext

internal expect class PlatformTypeFace

internal expect fun decodePlatformTypeFace(fontId: String, bytes: ByteArray): PlatformTypeFace?

internal expect fun platformTypeFaceSupports(typeFace: PlatformTypeFace, charCode: Int): Boolean

internal expect fun resolvePlatformFallbackTypeFace(
    fontId: String,
    charCode: Int,
): PlatformTypeFace?

internal expect object FontCache {
    operator fun get(fontId: String): PlatformTypeFace?
    operator fun set(fontId: String, typeFace: PlatformTypeFace)
    fun getSystemFallback(fontId: String, charCode: Int): PlatformTypeFace?
    fun addSystemFallback(fontId: String, typeFace: PlatformTypeFace)
    fun clear()
}

internal const val FONT_ID_CJK_REGULAR = "CJK-Regular"
internal const val FONT_ID_CJK_FALLBACK = "CJK-Fallback"
internal const val FONT_ID_EMOJI_FALLBACK = "Emoji-Fallback"

private val unicodeFallbackFontIds = setOf(
    FONT_ID_CJK_REGULAR,
    FONT_ID_CJK_FALLBACK,
    FONT_ID_EMOJI_FALLBACK,
)

internal fun isUnicodeFallbackFontId(fontId: String): Boolean = fontId in unicodeFallbackFontIds

private fun fallbackCodePoint(fontId: String, charCode: Int?): Int {
    if (charCode != null && charCode.isValidUnicodeCodePoint()) return charCode
    return when (fontId) {
        FONT_ID_EMOJI_FALLBACK -> 0x1F60A
        FONT_ID_CJK_FALLBACK -> 0x2605
        else -> 0x4E2D
    }
}

private fun Int.isValidUnicodeCodePoint(): Boolean =
    this in 0..0x10FFFF && this !in 0xD800..0xDFFF

private fun registeredFallbackFontIds(fontId: String): List<String> = when (fontId) {
    FONT_ID_CJK_REGULAR -> listOf(FONT_ID_CJK_FALLBACK, FONT_ID_EMOJI_FALLBACK)
    FONT_ID_CJK_FALLBACK -> listOf(FONT_ID_EMOJI_FALLBACK, FONT_ID_CJK_REGULAR)
    FONT_ID_EMOJI_FALLBACK -> listOf(FONT_ID_CJK_FALLBACK, FONT_ID_CJK_REGULAR)
    else -> emptyList()
}
