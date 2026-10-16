package io.ratex

import java.lang.reflect.InvocationTargetException
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class RaTeXFallbackBridgeAndroidTest {
    @Test
    fun jni_rejects_mismatched_arrays_and_recovers() = runTest {
        RaTeXEngine.prepareFallbackMetrics()
        val nativeParse = RaTeXFallbackBridge::class.java.getDeclaredMethod(
            "nativeParse", String::class.java, Boolean::class.javaPrimitiveType,
            FloatArray::class.java, IntArray::class.java, DoubleArray::class.java,
        ).apply { isAccessible = true }
        val error = assertFailsWith<InvocationTargetException> {
            nativeParse.invoke(null, """\text{中}""", true, floatArrayOf(0f, 0f, 0f, 1f),
                intArrayOf('中'.code), doubleArrayOf(1.15, 0.8))
        }
        assertIs<RaTeXException>(error.cause)
        assertTrue(error.cause?.message?.contains("length") == true)
        val metrics = RaTeXFallbackMetrics.of(mapOf('中'.code to FallbackGlyphMetrics(1.15, 0.8, 0.12)))
        assertTrue(abs(RaTeXEngine.parseWithFallbackMetrics("""\text{中}""", metrics).width - 1.15) < 1e-9)
    }
}
