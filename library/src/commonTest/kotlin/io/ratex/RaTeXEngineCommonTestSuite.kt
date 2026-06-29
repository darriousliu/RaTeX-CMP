package io.ratex

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class RaTeXEngineCommonTestSuite : DisplayListCommonTestSuite() {
    @Test
    fun parse_compose_example_formulas(): TestResult = runTest {
        val formulas = listOf(
            """\frac{-b \pm \sqrt{b^2 - 4ac}}{2a}""" to true,
            """f(x)=\frac{a_0}{2}+\sum_{n=1}^{\infty}\left(a_n\cos\left(\frac{n\pi x}{L}\right)+b_n\sin\left(\frac{n\pi x}{L}\right)\right)=\frac{1}{\pi}\int_{-\pi}^{\pi}f(t)\,dt+\sum_{n=1}^{\infty}\left(\frac{1}{\pi}\int_{-\pi}^{\pi}f(t)\cos(nt)\,dt\right)\cos(nx)+\sum_{n=1}^{\infty}\left(\frac{1}{\pi}\int_{-\pi}^{\pi}f(t)\sin(nt)\,dt\right)\sin(nx)""" to true,
            """\mathbb{P}\left(\bigcup_{n=1}^{m}A_n\right)=\sum_{n=1}^{m}\mathbb{P}(A_n)-\sum_{1\le i<j\le m}\mathbb{P}(A_i\cap A_j)+\sum_{1\le i<j<k\le m}\mathbb{P}(A_i\cap A_j\cap A_k)-\cdots+(-1)^{m+1}\mathbb{P}\left(\bigcap_{n=1}^{m}A_n\right)""" to true,
            """x=a_0+\cfrac{1}{a_1+\cfrac{1}{a_2+\cfrac{1}{a_3+\cfrac{1}{a_4+\cfrac{1}{a_5+\cfrac{1}{a_6}}}}}}""" to true,
                """\sum_{i=1}^{n}\left(\int_{0}^{1}\frac{\sum_{j=1}^{m}\left(\frac{x^{i+j}}{1+x^{2j}}\right)}{\sqrt{1+\sum_{k=1}^{r}\left(\frac{x^{2k}}{k^2}\right)}}\,dx\right)^{\!2}""" to true,
            """T(x)=\begin{cases}\begin{pmatrix}1&x&x^2\\0&1&2x\\0&0&1\end{pmatrix},&x\ge 0\\[1.2em]\begin{pmatrix}1&0&0\\-x&1&0\\x^2&-2x&1\end{pmatrix},&x<0\end{cases}""" to true,
            """e^{i\pi} + 1 = 0""" to false,
            """\ce{CO2 + C -> 2 CO}\qquad \pu{5.3e-11 m}""" to true,
            """\cancel{x}+\bcancel{y}+\xcancel{z}\qquad \overbrace{a+b+\cdots+z}^{26}""" to true,
            """E = mc^2 \tag{1}""" to true,
            """\begin{array}{c:c} a & b \\ \hdashline c & d \end{array}""" to true,
            """\mathbb{R}\quad \mathcal{F}\quad \mathfrak{g}\quad \mathsf{ABC}\quad \mathtt{code}""" to true,
            """\textcolor{#1565c0}{blue}\quad \color{orange}{orange}\quad \textcolor[RGB]{178,34,34}{firebrick}""" to true,
            """\begin{align} a&=b+c\nonumber\\ d&=e+f \end{align}""" to true,
            """\textcolor{#1565c0}{\text{status ✅ ⭐ 😊}}\quad x+y=z""" to true,
            """a \coloneqq b\qquad \llbracket x \rrbracket\qquad x \nshortmid y""" to true,
            """\textstyle \sum_{n=1}^{\infty}\frac{1}{n^2}=\frac{\pi^2}{6}""" to true,
        )

        formulas.forEach { (latex, displayMode) ->
            val displayList = parseFormula(latex, displayMode)
            assertTrue(displayList.items.isNotEmpty(), "Expected parsed items for $latex")
        }
    }

    @Test
    fun parse_0_1_5_regression_formulas(): TestResult = runTest {
        val formulas = listOf(
            """\displaystyle \iiint_{-\infty}^{\infty}""",
            """\textstyle\sum_{n=1}^{\infty}""",
            """\textstyle\sum\limits_{n=1}^{\infty}""",
            """\left( \htmlStyle{color: red;}{x \middle| y} \right)""",
            """\htmlStyle{color: red;}{x}^2""",
            """a \htmlStyle{color: red;}{+} b""",
        )

        formulas.forEach { latex ->
            val displayList = parseFormula(latex, displayMode = true)
            assertTrue(displayList.items.isNotEmpty(), "Expected parsed items for $latex")
        }
    }

    @Test
    fun parse_0_1_8_regression_formulas(): TestResult = runTest {
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
            val displayList = parseFormula(latex, displayMode = true)
            assertTrue(displayList.items.isNotEmpty(), "Expected parsed items for $latex")
        }
    }

    @Test
    fun parse_0_1_9_prooftree_formulas(): TestResult = runTest {
        val formulas = listOf(
            """\begin{prooftree}\AxiomC{P}\UnaryInfC{Q}\end{prooftree}""",
            """\begin{prooftree}\AxiomC{A \fCenter B}\RightLabel{r}\UnaryInfC{C}\end{prooftree}""",
            """\begin{prooftree}\AxiomC{P}\AxiomC{Q}\BinaryInfC{R}\end{prooftree}""",
            """\begin{prooftree}\AxiomC{P}\rootAtTop\UIC{Q}\end{prooftree}""",
        )

        formulas.forEach { latex ->
            val displayList = parseFormula(latex, displayMode = true)
            assertTrue(displayList.items.isNotEmpty(), "Expected parsed items for $latex")
        }
    }

    @Test
    fun parse_0_1_11_verb_multibyte_delimiters(): TestResult = runTest {
        val formulas = listOf(
            """\verb|hello|""",
            """\verbéxé""",
            """\verb*éx yé""",
        )

        formulas.forEach { latex ->
            val displayList = parseFormula(latex, displayMode = true)
            assertTrue(displayList.items.isNotEmpty(), "Expected parsed items for $latex")
        }
    }

    @Test
    fun parse_0_1_12_regression_formulas(): TestResult = runTest {
        val formulas = listOf(
            """\textcolor{#ff000080}{x} + y""",
            """\textcolor{#f008}{x} + y""",
            """a\dotsc,b""",
            """a\dotsc;b""",
            """\href{https://example.com}{x}""",
            """\left( \html@mathml{x \middle| y}{x} \right)""",
            """x\widetilde{x}""",
        )

        formulas.forEach { latex ->
            val displayList = parseFormula(latex, displayMode = true)
            assertTrue(displayList.items.isNotEmpty(), "Expected parsed items for $latex")
        }
    }

    @Test
    fun prooftree_line_styles_decode_to_display_lines(): TestResult = runTest {
        val dashedDisplayList = parseFormula(
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

        val noLineDisplayList = parseFormula(
            latex = """\begin{prooftree}\AxiomC{P}\noLine\UnaryInfC{Q}\end{prooftree}""",
            displayMode = true,
        )
        assertTrue(
            noLineDisplayList.items.none { item -> item is DisplayItem.Line },
            "Expected proof tree noLine to emit no DisplayItem.Line",
        )
    }

    @Test
    fun inline_explicit_limits_change_formula_metrics(): TestResult = runTest {
        val defaultLimits = parseFormula(
            latex = """\sum_{n=1}^{\infty}""",
            displayMode = false,
        )
        val explicitLimits = parseFormula(
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
    fun html_style_emits_styled_display_items(): TestResult = runTest {
        val displayList = parseFormula(
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
    fun transparent_color_decodes_alpha(): TestResult = runTest {
        val displayList = parseFormula(
            latex = """\textcolor{transparent}{x} + y""",
            displayMode = true,
        )

        val glyphColors = displayList.items
            .mapNotNull { item -> (item as? DisplayItem.GlyphPath)?.color }

        assertTrue(glyphColors.any { it.a == 0f }, "Expected transparent color alpha")
        assertTrue(
            glyphColors.any { it.a == 1f },
            "Expected opaque glyphs outside transparent group"
        )
    }

    @Test
    fun parse_0_1_12_hex_rgba_colors_decode_alpha(): TestResult = runTest {
        val longHex = parseFormula(
            latex = """\textcolor{#ff000080}{x} + y""",
            displayMode = true,
        )
        val shortHex = parseFormula(
            latex = """\textcolor{#f008}{x} + y""",
            displayMode = true,
        )

        val longHexColor = longHex.glyphColors().first { it.a < 1f }
        val shortHexColor = shortHex.glyphColors().first { it.a < 1f }

        assertFloatClose(128f / 255f, longHexColor.a, "Expected #rrggbbaa alpha to decode")
        assertFloatClose(136f / 255f, shortHexColor.a, "Expected #rgba alpha to decode")
    }

    @Test
    fun parse_0_1_12_dotsc_spacing_depends_on_following_punctuation(): TestResult = runTest {
        val dotscBeforeComma = parseFormula("""a\dotsc,b""", displayMode = true)
        val ldotsBeforeComma = parseFormula("""a\ldots,b""", displayMode = true)
        val dotscBeforeSemicolon = parseFormula("""a\dotsc;b""", displayMode = true)
        val ldotsBeforeSemicolon = parseFormula("""a\ldots;b""", displayMode = true)

        assertDoubleClose(
            ldotsBeforeComma.width,
            dotscBeforeComma.width,
            "Expected \\dotsc before comma not to add extra thin space",
        )
        assertTrue(
            dotscBeforeSemicolon.width > ldotsBeforeSemicolon.width + 0.05,
            "Expected \\dotsc before semicolon to add thin space: dotsc=${dotscBeforeSemicolon.width}, ldots=${ldotsBeforeSemicolon.width}",
        )
    }

    @Test
    fun parse_0_1_12_href_keeps_link_underline(): TestResult = runTest {
        val displayList = parseFormula(
            latex = """\href{https://example.com}{x}""",
            displayMode = true,
        )

        assertTrue(
            displayList.items.any { item ->
                val line = item as? DisplayItem.Line
                line?.color?.b == 1f && line.color.r == 0f && line.color.g == 0f
            },
            "Expected href body to emit a blue underline line",
        )
    }

    @Test
    fun parse_0_1_12_htmlmathml_middle_branch_renders_delimiter(): TestResult = runTest {
        val plain = parseFormula(
            latex = """\left( x \middle| y \right)""",
            displayMode = true,
        )
        val wrapped = parseFormula(
            latex = """\left( \html@mathml{x \middle| y}{x} \right)""",
            displayMode = true,
        )

        assertEquals(
            plain.items.size,
            wrapped.items.size,
            "Expected html@mathml html branch to render the current-pass middle delimiter",
        )
    }

    @Test
    fun parse_0_1_12_widetilde_path_stays_inside_display_width(): TestResult = runTest {
        val displayList = parseFormula(
            latex = """x\widetilde{x}""",
            displayMode = true,
        )
        val maxPathX = displayList.maxPathX()

        assertTrue(maxPathX.isFinite(), "Expected widetilde to emit a path")
        assertTrue(
            maxPathX <= displayList.width + 0.002,
            "Expected widetilde path to stay inside display width: maxPathX=$maxPathX, width=${displayList.width}",
        )
    }

    @Test
    fun parse_respects_color_without_overriding_explicit_latex_color(): TestResult = runTest {
        val displayList = parseFormula(
            latex = """x + \color{red}{y}""",
            displayMode = true,
            color = Color.Blue,
        )

        val glyphColors = displayList.items
            .mapNotNull { item -> (item as? DisplayItem.GlyphPath)?.color }

        assertTrue(glyphColors.any { it.b == 1f && it.r == 0f }, "Expected default blue glyphs")
        assertTrue(
            glyphColors.any { it.r == 1f && it.g == 0f && it.b == 0f },
            "Expected explicit red glyphs"
        )
        assertEquals(1f, glyphColors.first().a)
    }

    @Test
    fun parse_unicode_text_uses_cjk_regular_font(): TestResult = runTest {
        val displayList = parseFormula(
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

    protected open suspend fun beforeParse() = Unit

    private suspend fun parseFormula(
        latex: String,
        displayMode: Boolean = true,
        color: Color = Color.Black,
    ): DisplayList {
        beforeParse()
        return RaTeXEngine.parse(latex, displayMode, color)
    }

    private fun DisplayList.glyphColors(): List<RaTeXColor> =
        items.mapNotNull { item -> (item as? DisplayItem.GlyphPath)?.color }

    private fun DisplayList.maxPathX(): Double {
        var maxPathX = Double.NEGATIVE_INFINITY
        items.forEach { item ->
            val path = item as? DisplayItem.Path ?: return@forEach
            path.commands.forEach { command ->
                when (command) {
                    is PathCommand.MoveTo -> maxPathX = maxOf(maxPathX, path.x + command.x)
                    is PathCommand.LineTo -> maxPathX = maxOf(maxPathX, path.x + command.x)
                    is PathCommand.CubicTo -> {
                        maxPathX = maxOf(
                            maxPathX,
                            path.x + command.x1,
                            path.x + command.x2,
                            path.x + command.x,
                        )
                    }

                    is PathCommand.QuadTo -> {
                        maxPathX = maxOf(maxPathX, path.x + command.x1, path.x + command.x)
                    }

                    PathCommand.Close -> Unit
                }
            }
        }
        return maxPathX
    }

    private fun assertFloatClose(expected: Float, actual: Float, message: String) {
        assertTrue(abs(expected - actual) < 0.01f, "$message: expected=$expected, actual=$actual")
    }

    private fun assertDoubleClose(expected: Double, actual: Double, message: String) {
        assertTrue(abs(expected - actual) < 0.002, "$message: expected=$expected, actual=$actual")
    }
}
