package io.ratex

import androidx.compose.ui.graphics.Color
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@Structure.FieldOrder(
    "r",
    "g",
    "b",
    "a",
)
internal class RatexColorStruct() : Structure() {
    @JvmField
    var r: Float = 0f

    @JvmField
    var g: Float = 0f

    @JvmField
    var b: Float = 0f

    @JvmField
    var a: Float = 1f

    constructor(color: Color) : this() {
        r = color.red
        g = color.green
        b = color.blue
        a = color.alpha
    }
}

@Structure.FieldOrder(
    "struct_size",
    "display_mode",
    "color",
)
internal class RatexOptions : Structure() {
    @JvmField
    var struct_size: Long = 0

    @JvmField
    var display_mode: Int = 1

    @JvmField
    var color: Pointer? = null

    init {
        struct_size = size().toLong()
    }
}

@Structure.FieldOrder("data", "error_code")
internal open class RatexResult : Structure() {
    @JvmField
    var data: Pointer? = null

    @JvmField
    var error_code: Int = 0

    class ByValue : RatexResult(), Structure.ByValue
}

internal interface RaTeXNative : Library {
    fun ratex_parse_and_layout(latex: String, opts: RatexOptions?): RatexResult.ByValue
    fun ratex_free_display_list(json: Pointer?)
    fun ratex_get_last_error(): String?

    companion object {
        private const val LIBRARY_NAME = "ratex_ffi"

        val instance: RaTeXNative by lazy {
            loadBundledOrSystem()
        }

        private fun loadBundledOrSystem(): RaTeXNative {
            val extracted = extractBundledLibrary()
            return if (extracted != null) {
                Native.load(extracted.absolutePath, RaTeXNative::class.java)
            } else {
                Native.load(LIBRARY_NAME, RaTeXNative::class.java)
            }
        }

        private fun extractBundledLibrary(): File? {
            val mappedName = System.mapLibraryName(LIBRARY_NAME)
            val resourcePath = resourceCandidates(mappedName).firstNotNullOfOrNull { candidate ->
                RaTeXNative::class.java.classLoader.getResource(candidate)?.let { candidate }
            } ?: return null

            val cacheRoot = cacheRootDir().resolve(hostNativeDir())
            cacheRoot.mkdirs()

            val extractedFile = cacheRoot.resolve(mappedName)
            RaTeXNative::class.java.classLoader.getResourceAsStream(resourcePath)?.use { input ->
                Files.copy(input, extractedFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            } ?: return null

            extractedFile.setReadable(true)
            extractedFile.setExecutable(true)
            return extractedFile
        }

        private fun resourceCandidates(mappedName: String): List<String> {
            val dir = hostNativeDir()
            return listOf(
                "META-INF/ratex/native/$dir/$mappedName",
                "$dir/$mappedName",
            )
        }

        private fun hostNativeDir(): String {
            val osName = System.getProperty("os.name").lowercase()
            val normalizedArch = when (val archName = System.getProperty("os.arch").lowercase()) {
                "aarch64", "arm64" -> "aarch64"
                "x86_64", "amd64" -> "x86-64"
                else -> error("Unsupported desktop architecture: $archName")
            }

            return when {
                "mac" in osName -> "darwin-$normalizedArch"
                "linux" in osName -> "linux-$normalizedArch"
                "windows" in osName -> "windows-$normalizedArch"
                else -> error("Unsupported desktop OS: $osName")
            }
        }

        private fun cacheRootDir(): File {
            val osName = System.getProperty("os.name").lowercase()
            val userHome = File(System.getProperty("user.home"))
            return when {
                "mac" in osName -> userHome.resolve("Library/Caches/RaTeX/jna")
                "windows" in osName -> {
                    val localAppData = System.getenv("LOCALAPPDATA")
                    if (localAppData.isNullOrBlank()) {
                        userHome.resolve("AppData/Local/RaTeX/jna")
                    } else {
                        File(localAppData).resolve("RaTeX/jna")
                    }
                }
                else -> userHome.resolve(".cache/ratex/jna")
            }
        }
    }
}

internal actual object RaTeXEngine {
    private val native: RaTeXNative = RaTeXNative.instance
    actual suspend fun parse(
        latex: String,
        displayMode: Boolean,
        color: Color,
    ): DisplayList = withContext(Dispatchers.Default) { parseBlocking(latex, displayMode, color) }

    actual fun parseBlocking(
        latex: String,
        displayMode: Boolean,
        color: Color,
    ): DisplayList {
        val nativeColor = RatexColorStruct(color).also { it.write() }
        val opts = RatexOptions().also {
            it.display_mode = if (displayMode) 1 else 0
            it.color = nativeColor.pointer
            it.write()
        }
        val result = native.ratex_parse_and_layout(latex, opts)
        val ptr = if (result.error_code == 0) {
            result.data ?: throw RaTeXException("native parse returned null display list")
        } else {
            throw RaTeXException(
                native.ratex_get_last_error() ?: "native parse failed with error_code=${result.error_code}"
            )
        }
        val json: String
        try {
            json = ptr.getString(0, "UTF-8")
        } finally {
            native.ratex_free_display_list(ptr)
        }
        return try {
            ratexJson.decodeFromString(DisplayList.serializer(), json)
        } catch (e: Exception) {
            throw RaTeXException("JSON decode failed: ${e.message}")
        }
    }
}
