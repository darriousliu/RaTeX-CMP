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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
    val version: String,
    val label: String,
    val latex: String,
)

private const val ShowcaseVersionAll = "All"
private const val ShowcaseVersionCommon = "Common"

private val showcaseInlineParagraphs = listOf(
    $$"""Einstein showed that mass and energy are $E = mc^2$, where $c$ is the speed of light.""",
    $$"""A circle of radius $r$ has area $S = \pi r^2$ and circumference $C = 2\pi r$.""",
    """The golden ratio $\varphi = \frac{1+\sqrt{5}}{2}$ satisfies $\varphi^2 = \varphi + 1$.""",
    $$"""If $A = \begin{pmatrix} a & b \\ c & d \end{pmatrix}$, then $\det A = ad - bc$.""",
    """中文：勾股定理是 $\text{勾股定理：} a^2+b^2=c^2$。""",
    """行内中文：平均速度是 $\frac{\text{路程}}{\text{时间}}=\text{速度}$。""",
    """CJK fallback：$\text{中文かな한글} + x^2$ keeps inline size stable.""",
    """Emoji fallback：$\text{状态 ✅ ⭐ 😊} + n = 3$ stays baseline-aligned.""",
)

private val showcaseBlockSamples = listOf(
    ShowcaseBlockSample(
        version = ShowcaseVersionCommon,
        label = "Fourier transform",
        latex = """\hat{f}(\xi) = \int_{-\infty}^{\infty} f(x)\,e^{-2\pi i x \xi}\,dx""",
    ),
    ShowcaseBlockSample(
        version = ShowcaseVersionCommon,
        label = "3D rotation matrix",
        latex = """R_z(\theta)=\begin{pmatrix}\cos\theta&-\sin\theta&0\\\sin\theta&\cos\theta&0\\0&0&1\end{pmatrix}""",
    ),
    ShowcaseBlockSample(
        version = ShowcaseVersionCommon,
        label = "Schrodinger equation",
        latex = """i\hbar\frac{\partial}{\partial t}\Psi = \left[-\frac{\hbar^2}{2m}\nabla^2 + V\right]\Psi""",
    ),
    ShowcaseBlockSample(
        version = ShowcaseVersionCommon,
        label = """Residue theorem · \operatorname""",
        latex = """\oint_C f(z)\,dz = 2\pi i \sum_k \operatorname{Res}(f,z_k)""",
    ),
    ShowcaseBlockSample(
        version = ShowcaseVersionCommon,
        label = """CJK text · \text""",
        latex = """\text{中文公式：} E = mc^2,\quad \text{半径} = r,\quad \text{面积} = \pi r^2""",
    ),
    ShowcaseBlockSample(
        version = ShowcaseVersionCommon,
        label = "CJK fraction labels",
        latex = """\frac{\text{路程}}{\text{时间}} = \text{速度},\qquad \frac{\text{质量}}{\text{体积}} = \text{密度}""",
    ),
    ShowcaseBlockSample(
        version = ShowcaseVersionCommon,
        label = "Mixed CJK scripts",
        latex = """\text{中文：函数}\ f(x)=x^2,\quad \text{かな：せきぶん}\ \int_0^1 x\,dx=\frac{1}{2}""",
    ),
    ShowcaseBlockSample(
        version = ShowcaseVersionCommon,
        label = "Emoji fallback",
        latex = """\text{状态：✅ 成功，⭐ 收藏，😊 反馈}\quad x+y=z""",
    ),
    ShowcaseBlockSample(
        version = "0.1.0",
        label = """0.1.0 mhchem · \ce / \pu""",
        latex = """\ce{CO2 + C -> 2 CO}\qquad \pu{5.3e-11 m}""",
    ),
    ShowcaseBlockSample(
        version = "0.1.0",
        label = """0.1.0 cancel and braces""",
        latex = """\cancel{x}+\bcancel{y}+\xcancel{z}\qquad \overbrace{a+b+\cdots+z}^{26}""",
    ),
    ShowcaseBlockSample(
        version = "0.1.0",
        label = """0.1.0 tagged equation""",
        latex = """E = mc^2 \tag{1}""",
    ),
    ShowcaseBlockSample(
        version = "0.1.1",
        label = """0.1.1 dashed array line""",
        latex = """\begin{array}{c:c} a & b \\ \hdashline c & d \end{array}""",
    ),
    ShowcaseBlockSample(
        version = "0.1.2",
        label = "0.1.2 KaTeX font families",
        latex = """\mathbb{R}\quad \mathcal{F}\quad \mathfrak{g}\quad \mathsf{ABC}\quad \mathtt{code}""",
    ),
    ShowcaseBlockSample(
        version = "0.1.3",
        label = "0.1.3 custom color",
        latex = """\textcolor{#1565c0}{blue}\quad \color{orange}{orange}\quad \textcolor[RGB]{178,34,34}{firebrick}""",
    ),
    ShowcaseBlockSample(
        version = "0.1.3",
        label = "0.1.3 nonumber in aligned equations",
        latex = """\begin{align} a&=b+c\nonumber\\ d&=e+f \end{align}""",
    ),
    ShowcaseBlockSample(
        version = "0.1.5",
        label = "0.1.5 triple integral metrics",
        latex = """\displaystyle \iiint_{-\infty}^{\infty}""",
    ),
    ShowcaseBlockSample(
        version = "0.1.5",
        label = "0.1.5 inline sum default limits",
        latex = """\sum_{n=1}^{\infty}""",
    ),
    ShowcaseBlockSample(
        version = "0.1.5",
        label = "0.1.5 inline sum explicit limits",
        latex = """\sum\limits_{n=1}^{\infty}""",
    ),
    ShowcaseBlockSample(
        version = "0.1.5",
        label = "0.1.5 htmlStyle color",
        latex = """a \htmlStyle{color: red;}{+} b""",
    ),
    ShowcaseBlockSample(
        version = "0.1.5",
        label = "0.1.5 htmlStyle with middle",
        latex = """\left( \htmlStyle{color: red;}{x \middle| y} \right)""",
    ),
    ShowcaseBlockSample(
        version = "0.1.5",
        label = "0.1.5 htmlStyle superscript",
        latex = """\htmlStyle{color: red;}{x}^2""",
    ),
    ShowcaseBlockSample(
        version = "0.1.5",
        label = "0.1.5 transparent color",
        latex = """\textcolor{transparent}{x} + y""",
    ),
    ShowcaseBlockSample(
        version = "0.1.6",
        label = "0.1.6 emoji transparency fallback",
        latex = """\textcolor{#1565c0}{\text{status ✅ ⭐ 😊}}\quad x+y=z""",
    ),
    ShowcaseBlockSample(
        version = "0.1.7",
        label = "0.1.7 additional symbols",
        latex = """a \coloneqq b\qquad \llbracket x \rrbracket\qquad x \nshortmid y""",
    ),
    ShowcaseBlockSample(
        version = "0.1.8",
        label = "0.1.8 inline textstyle",
        latex = """\textstyle \sum_{n=1}^{\infty}\frac{1}{n^2}=\frac{\pi^2}{6}""",
    ),
    ShowcaseBlockSample(
        version = "0.1.9",
        label = "0.1.9 proof tree unary",
        latex = """\begin{prooftree}\AxiomC{P}\RightLabel{r}\UnaryInfC{Q}\end{prooftree}""",
    ),
    ShowcaseBlockSample(
        version = "0.1.9",
        label = "0.1.9 proof tree dashed",
        latex = """\begin{prooftree}\AxiomC{A \fCenter B}\AxiomC{B \fCenter C}\dashedLine\BinaryInfC{A \fCenter C}\end{prooftree}""",
    ),
    ShowcaseBlockSample(
        version = "0.1.9",
        label = "0.1.9 proof tree rootAtTop",
        latex = """\begin{prooftree}\AxiomC{P}\rootAtTop\UIC{Q}\end{prooftree}""",
    ),
    ShowcaseBlockSample(
        version = "0.1.10",
        label = "0.1.10 vertical guard",
        latex = """x = \frac{-b \pm \sqrt{b^2-4ac}}{2a}""",
    ),
    ShowcaseBlockSample(
        version = "0.1.11",
        label = """0.1.11 \verb multibyte delimiter""",
        latex = """\verbéx+yé""",
    ),
    ShowcaseBlockSample(
        version = "0.1.12",
        label = "0.1.12 hex RGBA color",
        latex = """\textcolor{#ff000080}{x} + \textcolor{#f008}{y} + z""",
    ),
    ShowcaseBlockSample(
        version = "0.1.12",
        label = """0.1.12 \dotsc punctuation""",
        latex = """a\dotsc,b\quad a\dotsc;b""",
    ),
    ShowcaseBlockSample(
        version = "0.1.12",
        label = "0.1.12 href underline",
        latex = """\href{https://example.com}{x+y=z}""",
    ),
    ShowcaseBlockSample(
        version = "0.1.12",
        label = """0.1.12 html@mathml \middle""",
        latex = """\left( \html@mathml{x \middle| y}{x} \right)""",
    ),
    ShowcaseBlockSample(
        version = "0.1.12",
        label = "0.1.12 widetilde path bounds",
        latex = """x\widetilde{x}""",
    ),
)

private val showcaseVersionFilters = listOf(ShowcaseVersionAll) +
    showcaseBlockSamples.map { it.version }.distinct()

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
    var selectedVersion by rememberSaveable { mutableStateOf(ShowcaseVersionAll) }
    val showInlineExamples = selectedVersion == ShowcaseVersionAll ||
        selectedVersion == ShowcaseVersionCommon
    val visibleBlockSamples = remember(selectedVersion) {
        if (selectedVersion == ShowcaseVersionAll) {
            showcaseBlockSamples
        } else {
            showcaseBlockSamples.filter { it.version == selectedVersion }
        }
    }

    Card {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            VersionFilterDropdown(
                selectedVersion = selectedVersion,
                onVersionSelected = { selectedVersion = it },
            )

            if (showInlineExamples) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Label("Common · inline layout baseline alignment")
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
            }

            visibleBlockSamples.forEach { sample ->
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
private fun VersionFilterDropdown(
    selectedVersion: String,
    onVersionSelected: (String) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
        ) {
            Text(selectedVersion.filterLabel())
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            showcaseVersionFilters.forEach { version ->
                DropdownMenuItem(
                    text = { Text(version.filterLabel()) },
                    onClick = {
                        onVersionSelected(version)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun String.filterLabel(): String =
    when (this) {
        ShowcaseVersionAll -> "All examples"
        ShowcaseVersionCommon -> "Common examples"
        else -> "$this additions"
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
            val displayList = parseResult.getOrNull()
            val fontSizePx = with(density) { mathFontSize.toPx() }
            val measured = remember(displayList, fontSizePx) {
                displayList?.measure(fontSizePx)
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
                        displayList = displayList,
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
