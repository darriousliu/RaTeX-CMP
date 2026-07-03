package io.ratex.compose.example

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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

private data class ShowcaseSample(
    val version: String,
    val label: String,
    val content: ShowcaseContent,
)

private sealed interface ShowcaseContent {
    data class InlineParagraphs(
        val paragraphs: List<String>,
        val mathFontSize: TextUnit = 19.sp,
    ) : ShowcaseContent

    data class BlockFormula(
        val latex: String,
        val fontSize: TextUnit = 20.sp,
    ) : ShowcaseContent

    data class BaselineRows(
        val rows: List<BaselineRow>,
        val fontSize: TextUnit = 20.sp,
    ) : ShowcaseContent
}

private data class BaselineRow(
    val label: String,
    val beforeText: String,
    val latex: String,
    val afterText: String,
)

private const val ShowcaseVersionAll = "All"
private const val ShowcaseVersionCommon = "Common"

private val showcaseSamples = listOf(
    ShowcaseSample(
        version = ShowcaseVersionCommon,
        label = "Common · inline layout baseline alignment",
        content = ShowcaseContent.InlineParagraphs(
            paragraphs = listOf(
                $$"""Einstein showed that mass and energy are $E = mc^2$, where $c$ is the speed of light.""",
                $$"""A circle of radius $r$ has area $S = \pi r^2$ and circumference $C = 2\pi r$.""",
                """The golden ratio $\varphi = \frac{1+\sqrt{5}}{2}$ satisfies $\varphi^2 = \varphi + 1$.""",
                $$"""If $A = \begin{pmatrix} a & b \\ c & d \end{pmatrix}$, then $\det A = ad - bc$.""",
                """中文：勾股定理是 $\text{勾股定理：} a^2+b^2=c^2$。""",
                """行内中文：平均速度是 $\frac{\text{路程}}{\text{时间}}=\text{速度}$。""",
                """CJK fallback：$\text{中文かな한글} + x^2$ keeps inline size stable.""",
                """Emoji fallback：$\text{状态 ✅ ⭐ 😊} + n = 3$ stays baseline-aligned.""",
            ),
        ),
    ),
    ShowcaseSample(
        version = ShowcaseVersionCommon,
        label = "Fourier transform",
        content = ShowcaseContent.BlockFormula("""\hat{f}(\xi) = \int_{-\infty}^{\infty} f(x)\,e^{-2\pi i x \xi}\,dx"""),
    ),
    ShowcaseSample(
        version = ShowcaseVersionCommon,
        label = "3D rotation matrix",
        content = ShowcaseContent.BlockFormula("""R_z(\theta)=\begin{pmatrix}\cos\theta&-\sin\theta&0\\\sin\theta&\cos\theta&0\\0&0&1\end{pmatrix}"""),
    ),
    ShowcaseSample(
        version = ShowcaseVersionCommon,
        label = "Schrodinger equation",
        content = ShowcaseContent.BlockFormula("""i\hbar\frac{\partial}{\partial t}\Psi = \left[-\frac{\hbar^2}{2m}\nabla^2 + V\right]\Psi"""),
    ),
    ShowcaseSample(
        version = ShowcaseVersionCommon,
        label = """Residue theorem · \operatorname""",
        content = ShowcaseContent.BlockFormula("""\oint_C f(z)\,dz = 2\pi i \sum_k \operatorname{Res}(f,z_k)"""),
    ),
    ShowcaseSample(
        version = ShowcaseVersionCommon,
        label = """CJK text · \text""",
        content = ShowcaseContent.BlockFormula("""\text{中文公式：} E = mc^2,\quad \text{半径} = r,\quad \text{面积} = \pi r^2"""),
    ),
    ShowcaseSample(
        version = ShowcaseVersionCommon,
        label = "CJK fraction labels",
        content = ShowcaseContent.BlockFormula("""\frac{\text{路程}}{\text{时间}} = \text{速度},\qquad \frac{\text{质量}}{\text{体积}} = \text{密度}"""),
    ),
    ShowcaseSample(
        version = ShowcaseVersionCommon,
        label = "Mixed CJK scripts",
        content = ShowcaseContent.BlockFormula("""\text{中文：函数}\ f(x)=x^2,\quad \text{かな：せきぶん}\ \int_0^1 x\,dx=\frac{1}{2}"""),
    ),
    ShowcaseSample(
        version = ShowcaseVersionCommon,
        label = "Emoji fallback",
        content = ShowcaseContent.BlockFormula("""\text{状态：✅ 成功，⭐ 收藏，😊 反馈}\quad x+y=z"""),
    ),
    ShowcaseSample(
        version = "0.1.0",
        label = """0.1.0 mhchem · \ce / \pu""",
        content = ShowcaseContent.BlockFormula("""\ce{CO2 + C -> 2 CO}\qquad \pu{5.3e-11 m}"""),
    ),
    ShowcaseSample(
        version = "0.1.0",
        label = """0.1.0 cancel and braces""",
        content = ShowcaseContent.BlockFormula("""\cancel{x}+\bcancel{y}+\xcancel{z}\qquad \overbrace{a+b+\cdots+z}^{26}"""),
    ),
    ShowcaseSample(
        version = "0.1.0",
        label = """0.1.0 tagged equation""",
        content = ShowcaseContent.BlockFormula("""E = mc^2 \tag{1}"""),
    ),
    ShowcaseSample(
        version = "0.1.1",
        label = """0.1.1 dashed array line""",
        content = ShowcaseContent.BlockFormula("""\begin{array}{c:c} a & b \\ \hdashline c & d \end{array}"""),
    ),
    ShowcaseSample(
        version = "0.1.2",
        label = "0.1.2 KaTeX font families",
        content = ShowcaseContent.BlockFormula("""\mathbb{R}\quad \mathcal{F}\quad \mathfrak{g}\quad \mathsf{ABC}\quad \mathtt{code}"""),
    ),
    ShowcaseSample(
        version = "0.1.3",
        label = "0.1.3 custom color",
        content = ShowcaseContent.BlockFormula("""\textcolor{#1565c0}{blue}\quad \color{orange}{orange}\quad \textcolor[RGB]{178,34,34}{firebrick}"""),
    ),
    ShowcaseSample(
        version = "0.1.3",
        label = "0.1.3 nonumber in aligned equations",
        content = ShowcaseContent.BlockFormula("""\begin{align} a&=b+c\nonumber\\ d&=e+f \end{align}"""),
    ),
    ShowcaseSample(
        version = "0.1.5",
        label = "0.1.5 triple integral metrics",
        content = ShowcaseContent.BlockFormula("""\displaystyle \iiint_{-\infty}^{\infty}"""),
    ),
    ShowcaseSample(
        version = "0.1.5",
        label = "0.1.5 inline sum default limits",
        content = ShowcaseContent.BlockFormula("""\sum_{n=1}^{\infty}"""),
    ),
    ShowcaseSample(
        version = "0.1.5",
        label = "0.1.5 inline sum explicit limits",
        content = ShowcaseContent.BlockFormula("""\sum\limits_{n=1}^{\infty}"""),
    ),
    ShowcaseSample(
        version = "0.1.5",
        label = "0.1.5 htmlStyle color",
        content = ShowcaseContent.BlockFormula("""a \htmlStyle{color: red;}{+} b"""),
    ),
    ShowcaseSample(
        version = "0.1.5",
        label = "0.1.5 htmlStyle with middle",
        content = ShowcaseContent.BlockFormula("""\left( \htmlStyle{color: red;}{x \middle| y} \right)"""),
    ),
    ShowcaseSample(
        version = "0.1.5",
        label = "0.1.5 htmlStyle superscript",
        content = ShowcaseContent.BlockFormula("""\htmlStyle{color: red;}{x}^2"""),
    ),
    ShowcaseSample(
        version = "0.1.5",
        label = "0.1.5 transparent color",
        content = ShowcaseContent.BlockFormula("""\textcolor{transparent}{x} + y"""),
    ),
    ShowcaseSample(
        version = "0.1.6",
        label = "0.1.6 emoji transparency fallback",
        content = ShowcaseContent.BlockFormula("""\textcolor{#1565c0}{\text{status ✅ ⭐ 😊}}\quad x+y=z"""),
    ),
    ShowcaseSample(
        version = "0.1.7",
        label = "0.1.7 additional symbols",
        content = ShowcaseContent.BlockFormula("""a \coloneqq b\qquad \llbracket x \rrbracket\qquad x \nshortmid y"""),
    ),
    ShowcaseSample(
        version = "0.1.8",
        label = "0.1.8 inline textstyle",
        content = ShowcaseContent.BlockFormula("""\textstyle \sum_{n=1}^{\infty}\frac{1}{n^2}=\frac{\pi^2}{6}"""),
    ),
    ShowcaseSample(
        version = "0.1.9",
        label = "0.1.9 proof tree unary",
        content = ShowcaseContent.BlockFormula("""\begin{prooftree}\AxiomC{P}\RightLabel{r}\UnaryInfC{Q}\end{prooftree}"""),
    ),
    ShowcaseSample(
        version = "0.1.9",
        label = "0.1.9 proof tree dashed",
        content = ShowcaseContent.BlockFormula("""\begin{prooftree}\AxiomC{A \fCenter B}\AxiomC{B \fCenter C}\dashedLine\BinaryInfC{A \fCenter C}\end{prooftree}"""),
    ),
    ShowcaseSample(
        version = "0.1.9",
        label = "0.1.9 proof tree rootAtTop",
        content = ShowcaseContent.BlockFormula("""\begin{prooftree}\AxiomC{P}\rootAtTop\UIC{Q}\end{prooftree}"""),
    ),
    ShowcaseSample(
        version = "0.1.10",
        label = "0.1.10 vertical guard",
        content = ShowcaseContent.BlockFormula("""x = \frac{-b \pm \sqrt{b^2-4ac}}{2a}"""),
    ),
    ShowcaseSample(
        version = "0.1.11",
        label = """0.1.11 \verb multibyte delimiter""",
        content = ShowcaseContent.BlockFormula("""\verbéx+yé"""),
    ),
    ShowcaseSample(
        version = "0.1.12",
        label = "0.1.12 hex RGBA color",
        content = ShowcaseContent.BlockFormula("""\textcolor{#ff000080}{x} + \textcolor{#f008}{y} + z"""),
    ),
    ShowcaseSample(
        version = "0.1.12",
        label = """0.1.12 \dotsc punctuation""",
        content = ShowcaseContent.BlockFormula("""a\dotsc,b\quad a\dotsc;b"""),
    ),
    ShowcaseSample(
        version = "0.1.12",
        label = "0.1.12 href underline",
        content = ShowcaseContent.BlockFormula("""\href{https://example.com}{x+y=z}"""),
    ),
    ShowcaseSample(
        version = "0.1.12",
        label = """0.1.12 html@mathml \middle""",
        content = ShowcaseContent.BlockFormula("""\left( \html@mathml{x \middle| y}{x} \right)"""),
    ),
    ShowcaseSample(
        version = "0.1.12",
        label = "0.1.12 widetilde path bounds",
        content = ShowcaseContent.BlockFormula("""x\widetilde{x}"""),
    ),
    ShowcaseSample(
        version = "0.1.12-1",
        label = "0.1.12-1 · Compose Text + inline RaTeX baseline alignment",
        content = ShowcaseContent.BaselineRows(
            rows = listOf(
                BaselineRow(
                    label = """subscript baseline · x_i""",
                    beforeText = "The value ",
                    latex = """x_i""",
                    afterText = " sits on the same baseline.",
                ),
                BaselineRow(
                    label = """fraction baseline · \frac{a_i}{b_i}""",
                    beforeText = "The ratio ",
                    latex = """\frac{a_i}{b_i}""",
                    afterText = " keeps surrounding text aligned.",
                ),
                BaselineRow(
                    label = """radical baseline · \sqrt{x_i}""",
                    beforeText = "The root ",
                    latex = """\sqrt{x_i}""",
                    afterText = " no longer floats above text.",
                ),
            ),
        ),
    ),
)

private val showcaseVersionFilters = (
    listOf(ShowcaseVersionAll) +
        showcaseSamples.map { it.version }
    ).distinct()

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
    val visibleSamples = remember(selectedVersion) {
        if (selectedVersion == ShowcaseVersionAll) {
            showcaseSamples
        } else {
            showcaseSamples.filter { it.version == selectedVersion }
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

            visibleSamples.forEach { sample ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Label(sample.label)
                Spacer(modifier = Modifier.height(8.dp))
                ShowcaseSampleContent(sample.content)
            }
        }
    }
}

@Composable
private fun ShowcaseSampleContent(
    content: ShowcaseContent,
) {
    when (content) {
        is ShowcaseContent.InlineParagraphs -> InlineParagraphs(
            paragraphs = content.paragraphs,
            mathFontSize = content.mathFontSize,
        )

        is ShowcaseContent.BlockFormula -> BlockFormula(
            latex = content.latex,
            fontSize = content.fontSize,
        )

        is ShowcaseContent.BaselineRows -> BaselineRows(
            rows = content.rows,
            fontSize = content.fontSize,
        )
    }
}

@Composable
private fun InlineParagraphs(
    paragraphs: List<String>,
    mathFontSize: TextUnit,
) {
    Column {
        paragraphs.forEachIndexed { index, paragraph ->
            InlineMathText(
                text = paragraph,
                mathFontSize = mathFontSize,
            )
            if (index != paragraphs.lastIndex) {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun BaselineRows(
    rows: List<BaselineRow>,
    fontSize: TextUnit,
) {
    Column {
        rows.forEachIndexed { index, row ->
            BaselineAlignmentRow(
                row = row,
                fontSize = fontSize,
            )
            if (index != rows.lastIndex) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun BaselineAlignmentRow(
    row: BaselineRow,
    fontSize: TextUnit,
) {
    Column {
        Label(row.label)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) {
            Text(
                text = row.beforeText,
                fontSize = fontSize,
                modifier = Modifier.alignByBaseline(),
            )
            RaTeX(
                latex = row.latex,
                fontSize = fontSize,
                displayMode = false,
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                text = row.afterText,
                fontSize = fontSize,
                modifier = Modifier.alignByBaseline(),
            )
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
