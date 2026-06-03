package io.ratex

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RaTeXEngineJvmTest {
    @Test
    fun measure_adds_vertical_guard_for_non_empty_display_lists() {
        val displayList = DisplayList(
            width = 2.0,
            height = 3.0,
            depth = 0.5,
            items = listOf(
                DisplayItem.GlyphPath(
                    x = 0.0,
                    y = 0.0,
                    scale = 1.0,
                    font = "KaTeX_Main-Regular",
                    charCode = 'x'.code,
                    color = RaTeXColor(0f, 0f, 0f, 1f),
                ),
            ),
        )

        val measured = displayList.measure(fontSizePx = 10f)

        assertEquals(20f, measured.widthPx)
        assertEquals(31f, measured.heightPx)
        assertEquals(6f, measured.depthPx)
        assertEquals(37f, measured.totalHeightPx)
    }

    @Test
    fun measure_keeps_empty_display_lists_tight() {
        val displayList = DisplayList(
            width = 0.0,
            height = 0.0,
            depth = 0.0,
            items = emptyList(),
        )

        val measured = displayList.measure(fontSizePx = 10f)

        assertEquals(0f, measured.widthPx)
        assertEquals(0f, measured.heightPx)
        assertEquals(0f, measured.depthPx)
        assertEquals(0f, measured.totalHeightPx)
    }

    @Test
    fun parse_compose_example_formulas() {
        val formulas = listOf(
            """\frac{-b \pm \sqrt{b^2 - 4ac}}{2a}""" to true,
            """f(x)=\frac{a_0}{2}+\sum_{n=1}^{\infty}\left(a_n\cos\left(\frac{n\pi x}{L}\right)+b_n\sin\left(\frac{n\pi x}{L}\right)\right)=\frac{1}{\pi}\int_{-\pi}^{\pi}f(t)\,dt+\sum_{n=1}^{\infty}\left(\frac{1}{\pi}\int_{-\pi}^{\pi}f(t)\cos(nt)\,dt\right)\cos(nx)+\sum_{n=1}^{\infty}\left(\frac{1}{\pi}\int_{-\pi}^{\pi}f(t)\sin(nt)\,dt\right)\sin(nx)""" to true,
            """\mathbb{P}\left(\bigcup_{n=1}^{m}A_n\right)=\sum_{n=1}^{m}\mathbb{P}(A_n)-\sum_{1\le i<j\le m}\mathbb{P}(A_i\cap A_j)+\sum_{1\le i<j<k\le m}\mathbb{P}(A_i\cap A_j\cap A_k)-\cdots+(-1)^{m+1}\mathbb{P}\left(\bigcap_{n=1}^{m}A_n\right)""" to true,
            """x=a_0+\cfrac{1}{a_1+\cfrac{1}{a_2+\cfrac{1}{a_3+\cfrac{1}{a_4+\cfrac{1}{a_5+\cfrac{1}{a_6}}}}}}""" to true,
            """\sum_{i=1}^{n}\left(\int_{0}^{1}\frac{\sum_{j=1}^{m}\left(\frac{x^{i+j}}{1+x^{2j}}\right)}{\sqrt{1+\sum_{k=1}^{r}\left(\frac{x^{2k}}{k^2}\right)}}\,dx\right)^{\!2}""" to true,
            """T(x)=\begin{cases}\begin{pmatrix}1&x&x^2\\0&1&2x\\0&0&1\end{pmatrix},&x\ge 0\\[1.2em]\begin{pmatrix}1&0&0\\-x&1&0\\x^2&-2x&1\end{pmatrix},&x<0\end{cases}""" to true,
            """e^{i\pi} + 1 = 0""" to false,
        )

        formulas.forEach { (latex, displayMode) ->
            val displayList = RaTeXEngine.parseBlocking(latex, displayMode)
            assertTrue(displayList.items.isNotEmpty(), "Expected parsed items for $latex")
        }
    }

    @Test
    fun parse_0_1_5_regression_formulas() {
        val formulas = listOf(
            """\displaystyle \iiint_{-\infty}^{\infty}""",
            """\textstyle\sum_{n=1}^{\infty}""",
            """\textstyle\sum\limits_{n=1}^{\infty}""",
            """\left( \htmlStyle{color: red;}{x \middle| y} \right)""",
            """\htmlStyle{color: red;}{x}^2""",
            """a \htmlStyle{color: red;}{+} b""",
        )

        formulas.forEach { latex ->
            val displayList = RaTeXEngine.parseBlocking(latex, displayMode = true)
            assertTrue(displayList.items.isNotEmpty(), "Expected parsed items for $latex")
        }
    }

    @Test
    fun parse_0_1_8_regression_formulas() {
        val formulas = listOf(
            "a \u00B7 b",
            "\u222B_0^1 x\\,dx",
            "\u222C_D f(x,y)\\,dx\\,dy",
            """\text{\sout{removed}} + x""",
            "\u2254",
            "\u27E6 x \u27E7",
            "\u29B5",
            "\u00A9 + \u00AE",
        )

        formulas.forEach { latex ->
            val displayList = RaTeXEngine.parseBlocking(latex, displayMode = true)
            assertTrue(displayList.items.isNotEmpty(), "Expected parsed items for $latex")
        }
    }

    @Test
    fun parse_0_1_9_prooftree_formulas() {
        val formulas = listOf(
            """\begin{prooftree}\AxiomC{P}\UnaryInfC{Q}\end{prooftree}""",
            """\begin{prooftree}\AxiomC{A \fCenter B}\RightLabel{r}\UnaryInfC{C}\end{prooftree}""",
            """\begin{prooftree}\AxiomC{P}\AxiomC{Q}\BinaryInfC{R}\end{prooftree}""",
            """\begin{prooftree}\AxiomC{P}\rootAtTop\UIC{Q}\end{prooftree}""",
        )

        formulas.forEach { latex ->
            val displayList = RaTeXEngine.parseBlocking(latex, displayMode = true)
            assertTrue(displayList.items.isNotEmpty(), "Expected parsed items for $latex")
        }
    }

    @Test
    fun parse_0_1_11_verb_multibyte_delimiters() {
        val formulas = listOf(
            """\verb|hello|""",
            """\verbéxé""",
            """\verb*éx yé""",
        )

        formulas.forEach { latex ->
            val displayList = RaTeXEngine.parseBlocking(latex, displayMode = true)
            assertTrue(displayList.items.isNotEmpty(), "Expected parsed items for $latex")
        }
    }

    @Test
    fun parse_0_1_11_reports_recursion_limit() {
        val latex = "{".repeat(600) + "x" + "}".repeat(600)

        val error = assertFailsWith<RaTeXException> {
            RaTeXEngine.parseBlocking(latex, displayMode = true)
        }
        assertTrue(
            error.message?.contains("Recursion limit exceeded") == true,
            "Expected recursion limit error, got: ${error.message}",
        )
    }

    @Test
    fun prooftree_line_styles_decode_to_display_lines() {
        val dashedDisplayList = RaTeXEngine.parseBlocking(
            latex = """\begin{prooftree}\AxiomC{P}\dashedLine\UnaryInfC{Q}\end{prooftree}""",
            displayMode = true,
        )
        assertTrue(
            dashedDisplayList.items.any { item ->
                val line = item as? DisplayItem.Line
                line?.dashed == true
            },
            "Expected proof tree dashed line to decode as DisplayItem.Line(dashed = true)",
        )

        val noLineDisplayList = RaTeXEngine.parseBlocking(
            latex = """\begin{prooftree}\AxiomC{P}\noLine\UnaryInfC{Q}\end{prooftree}""",
            displayMode = true,
        )
        assertTrue(
            noLineDisplayList.items.none { item -> item is DisplayItem.Line },
            "Expected proof tree noLine to emit no DisplayItem.Line",
        )
    }

    @Test
    fun inline_explicit_limits_change_formula_metrics() {
        val defaultLimits = RaTeXEngine.parseBlocking(
            latex = """\sum_{n=1}^{\infty}""",
            displayMode = false,
        )
        val explicitLimits = RaTeXEngine.parseBlocking(
            latex = """\sum\limits_{n=1}^{\infty}""",
            displayMode = false,
        )

        assertTrue(
            explicitLimits.height > defaultLimits.height + 0.5,
            "Expected explicit inline limits to increase height: default=${defaultLimits.height}, explicit=${explicitLimits.height}",
        )
        assertTrue(
            explicitLimits.depth > defaultLimits.depth + 0.5,
            "Expected explicit inline limits to increase depth: default=${defaultLimits.depth}, explicit=${explicitLimits.depth}",
        )
    }

    @Test
    fun html_style_emits_styled_display_items() {
        val displayList = RaTeXEngine.parseBlocking(
            latex = """\htmlStyle{color: blue; background-color: yellow; text-decoration: underline;}{x}""",
            displayMode = true,
        )

        assertTrue(
            displayList.items.any { item ->
                val glyph = item as? DisplayItem.GlyphPath
                glyph?.color?.b == 1f && glyph.color.r == 0f && glyph.color.g == 0f
            },
            "Expected blue glyph from htmlStyle color",
        )
        assertTrue(
            displayList.items.any { item ->
                val rect = item as? DisplayItem.Rect
                rect?.color?.r == 1f && rect.color.g == 1f && rect.color.b == 0f
            },
            "Expected yellow background rect from htmlStyle background-color",
        )
        assertTrue(
            displayList.items.any { item ->
                val line = item as? DisplayItem.Line
                line?.color?.b == 1f && line.color.r == 0f && line.color.g == 0f
            },
            "Expected blue underline from htmlStyle text-decoration",
        )
    }

    @Test
    fun transparent_color_decodes_alpha() {
        val displayList = RaTeXEngine.parseBlocking(
            latex = """\textcolor{transparent}{x} + y""",
            displayMode = true,
        )

        val glyphColors = displayList.items
            .mapNotNull { item -> (item as? DisplayItem.GlyphPath)?.color }

        assertTrue(glyphColors.any { it.a == 0f }, "Expected transparent color alpha")
        assertTrue(glyphColors.any { it.a == 1f }, "Expected opaque glyphs outside transparent group")
    }

    @Test
    fun parse_respects_color_without_overriding_explicit_latex_color() {
        val displayList = RaTeXEngine.parseBlocking(
            latex = """x + \color{red}{y}""",
            displayMode = true,
            color = Color.Blue,
        )

        val glyphColors = displayList.items
            .mapNotNull { item -> (item as? DisplayItem.GlyphPath)?.color }

        assertTrue(glyphColors.any { it.b == 1f && it.r == 0f }, "Expected default blue glyphs")
        assertTrue(glyphColors.any { it.r == 1f && it.g == 0f && it.b == 0f }, "Expected explicit red glyphs")
        assertEquals(1f, glyphColors.first().a)
    }

    @Test
    fun parse_unicode_text_uses_cjk_regular_font() {
        val displayList = RaTeXEngine.parseBlocking(
            latex = "\\text{\u4E2D\u6587 \uD83D\uDE0A}",
            displayMode = true,
        )

        val glyphFonts = displayList.items
            .mapNotNull { item -> (item as? DisplayItem.GlyphPath)?.font }

        assertTrue(
            glyphFonts.contains(FONT_ID_CJK_REGULAR),
            "Expected $FONT_ID_CJK_REGULAR glyph font for Unicode fallback text, got $glyphFonts",
        )
    }

    @Test
    fun loads_cjk_platform_fallback_typeface() {
        runBlocking {
            RaTeXFontLoader.clear()
            RaTeXFontLoader.ensureLoaded()

            assertNotNull(
                RaTeXFontLoader.getPlatformTypeFace(FONT_ID_CJK_REGULAR, 0x4E2D),
                "Expected a platform typeface for $FONT_ID_CJK_REGULAR",
            )
        }
    }
}
