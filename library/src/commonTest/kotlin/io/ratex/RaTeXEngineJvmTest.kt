package io.ratex

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RaTeXEngineJvmTest {
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
}
