package io.ratex.compose.example

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ratex.compose.RaTeX
import io.ratex.compose.rememberBlockingRaTeXDisplayList
import io.ratex.measure

private data class ShowcaseBlockSample(
    val label: String,
    val latex: String,
)

private val showcaseInlineParagraphs = listOf(
    $$"""Einstein showed that mass and energy are $E = mc^2$, where $c$ is the speed of light.""",
    $$"""A circle of radius $r$ has area $S = \pi r^2$ and circumference $C = 2\pi r$.""",
    """The golden ratio $\varphi = \frac{1+\sqrt{5}}{2}$ satisfies $\varphi^2 = \varphi + 1$.""",
    $$"""If $A = \begin{pmatrix} a & b \\ c & d \end{pmatrix}$, then $\det A = ad - bc$.""",
)

private val showcaseBlockSamples = listOf(
    ShowcaseBlockSample(
        label = "Fourier transform",
        latex = """\hat{f}(\xi) = \int_{-\infty}^{\infty} f(x)\,e^{-2\pi i x \xi}\,dx""",
    ),
    ShowcaseBlockSample(
        label = "3D rotation matrix",
        latex = """R_z(\theta)=\begin{pmatrix}\cos\theta&-\sin\theta&0\\\sin\theta&\cos\theta&0\\0&0&1\end{pmatrix}""",
    ),
    ShowcaseBlockSample(
        label = "Schrodinger equation",
        latex = """i\hbar\frac{\partial}{\partial t}\Psi = \left[-\frac{\hbar^2}{2m}\nabla^2 + V\right]\Psi""",
    ),
    ShowcaseBlockSample(
        label = """Residue theorem · \operatorname""",
        latex = """\oint_C f(z)\,dz = 2\pi i \sum_k \operatorname{Res}(f,z_k)""",
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaTeXShowcasePage() {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("RaTeX Demo") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.inversePrimary,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            item(key = "headline") {
                SectionHeader("RaTeX · Native Cross-Platform Math")
            }
            item(key = "showcase") {
                ShowcaseCard()
            }
        }
    }
}

@Composable
private fun ShowcaseCard() {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Label("Inline layout · baseline alignment")
            Spacer(modifier = Modifier.height(6.dp))

            showcaseInlineParagraphs.forEachIndexed { index, paragraph ->
                InlineMathText(
                    text = paragraph,
                    mathFontSize = 19.sp,
                )
                if (index != showcaseInlineParagraphs.lastIndex) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            showcaseBlockSamples.forEach { sample ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Label(sample.label)
                Spacer(modifier = Modifier.height(8.dp))
                BlockFormula(
                    latex = sample.latex,
                    fontSize = 20.sp,
                )
            }
        }
    }
}

@Composable
private fun InlineMathText(
    text: String,
    mathFontSize: TextUnit = 18.sp,
) {
    val density = LocalDensity.current
    val inlineContent = remember { linkedMapOf<String, InlineTextContent>() }
    inlineContent.clear()
    val annotatedText = rememberInlineMathAnnotatedString(
        text = text,
        mathFontSize = mathFontSize,
        density = density,
        inlineContent = inlineContent,
    )

    Text(
        text = annotatedText,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = 16.sp,
            lineHeight = 28.8.sp,
            color = Color.Black.copy(alpha = 0.87f),
        ),
        inlineContent = inlineContent,
    )
}

@Composable
private fun rememberInlineMathAnnotatedString(
    text: String,
    mathFontSize: TextUnit,
    density: Density,
    inlineContent: MutableMap<String, InlineTextContent>,
): AnnotatedString {
    val builder = AnnotatedString.Builder()
    val parts = text.split('$')

    parts.forEachIndexed { index, part ->
        if (part.isEmpty()) return@forEachIndexed

        if (index % 2 == 0) {
            builder.append(part)
        } else {
            val placeholderId = "formula_$index"
            val parseResult = rememberBlockingRaTeXDisplayList(
                latex = part,
                displayMode = false,
            )
            val fontSizePx = with(density) { mathFontSize.toPx() }
            val measured = remember(parseResult, fontSizePx) {
                parseResult.getOrNull()?.measure(fontSizePx)
            }
            val width = with(density) { (measured?.widthPx ?: fontSizePx).toSp() }
            val height = with(density) { (measured?.totalHeightPx ?: fontSizePx).toSp() }

            inlineContent[placeholderId] = InlineTextContent(
                placeholder = Placeholder(
                    width = width,
                    height = height,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                ),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    RaTeX(
                        displayList = parseResult.getOrNull(),
                        fontSize = mathFontSize,
                    )
                }
            }
            builder.appendInlineContent(placeholderId, part)
        }
    }

    return builder.toAnnotatedString()
}

@Composable
private fun BlockFormula(
    latex: String,
    fontSize: TextUnit,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            RaTeX(
                latex = latex,
                fontSize = fontSize,
                displayMode = true,
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
) {
    Text(
        text = title,
        modifier = Modifier.padding(bottom = 8.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun Label(
    text: String,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = Color.Gray,
    )
}
