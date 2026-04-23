package io.ratex.compose.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ratex.DisplayList
import io.ratex.compose.RaTeX
import io.ratex.compose.rememberRaTeXDisplayList
import io.ratex.measure
import kotlin.random.Random

fun randomComposeColor(
    min: Float = 0.2f,
    max: Float = 0.8f,
    alpha: Float = 1f
): Color {
    fun r() = Random.nextFloat() * (max - min) + min
    return Color(
        red = r(),
        green = r(),
        blue = r(),
        alpha = alpha.coerceIn(0f, 1f)
    )
}

private data class FormulaSample(
    val id: String,
    val title: String,
    val latex: String,
    val displayMode: Boolean,
) {
    val color: Color
        get() = randomComposeColor()
}

private val sampleFormulas = listOf(
    FormulaSample(
        id = "quadratic",
        title = "Quadratic Formula",
        latex = """\frac{-b \pm \sqrt{b^2 - 4ac}}{2a}""",
        displayMode = true,
    ),
    FormulaSample(
        id = "euler",
        title = "Euler Identity",
        latex = """e^{i\pi} + 1 = 0""",
        displayMode = false,
    ),
    FormulaSample(
        id = "gaussian",
        title = "Gaussian Integral",
        latex = """\int_{-\infty}^{\infty} e^{-x^2} dx = \sqrt{\pi}""",
        displayMode = true,
    ),
    FormulaSample(
        id = "binomial",
        title = "Binomial Theorem",
        latex = """(x+y)^n = \sum_{k=0}^{n}\binom{n}{k}x^{n-k}y^k""",
        displayMode = true,
    ),
    FormulaSample(
        id = "long-fourier-series",
        title = "Long Fourier Expansion",
        latex = """f(x)=\frac{a_0}{2}+\sum_{n=1}^{\infty}\left(a_n\cos\left(\frac{n\pi x}{L}\right)+b_n\sin\left(\frac{n\pi x}{L}\right)\right)=\frac{1}{\pi}\int_{-\pi}^{\pi}f(t)\,dt+\sum_{n=1}^{\infty}\left(\frac{1}{\pi}\int_{-\pi}^{\pi}f(t)\cos(nt)\,dt\right)\cos(nx)+\sum_{n=1}^{\infty}\left(\frac{1}{\pi}\int_{-\pi}^{\pi}f(t)\sin(nt)\,dt\right)\sin(nx)""",
        displayMode = true,
    ),
    FormulaSample(
        id = "long-probability",
        title = "Long Probability Identity",
        latex = """\mathbb{P}\left(\bigcup_{n=1}^{m}A_n\right)=\sum_{n=1}^{m}\mathbb{P}(A_n)-\sum_{1\le i<j\le m}\mathbb{P}(A_i\cap A_j)+\sum_{1\le i<j<k\le m}\mathbb{P}(A_i\cap A_j\cap A_k)-\cdots+(-1)^{m+1}\mathbb{P}\left(\bigcap_{n=1}^{m}A_n\right)""",
        displayMode = true,
    ),
    FormulaSample(
        id = "tall-continued-fraction",
        title = "Tall Continued Fraction",
        latex = """x=a_0+\cfrac{1}{a_1+\cfrac{1}{a_2+\cfrac{1}{a_3+\cfrac{1}{a_4+\cfrac{1}{a_5+\cfrac{1}{a_6}}}}}}""",
        displayMode = true,
    ),
    FormulaSample(
        id = "tall-nested-sum",
        title = "Tall Nested Sum and Integral",
        latex = """\sum_{i=1}^{n}\left(\int_{0}^{1}\frac{\sum_{j=1}^{m}\left(\frac{x^{i+j}}{1+x^{2j}}\right)}{\sqrt{1+\sum_{k=1}^{r}\left(\frac{x^{2k}}{k^2}\right)}}\,dx\right)^{\!2}""",
        displayMode = true,
    ),
    FormulaSample(
        id = "piecewise-matrix",
        title = "Piecewise With Matrix",
        latex = """T(x)=\begin{cases}\begin{pmatrix}1&x&x^2\\0&1&2x\\0&0&1\end{pmatrix},&x\ge 0\\[1.2em]\begin{pmatrix}1&0&0\\-x&1&0\\x^2&-2x&1\end{pmatrix},&x<0\end{cases}""",
        displayMode = true,
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaTeXExampleApp() {
    val colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()

    MaterialTheme(colorScheme = colorScheme) {
//        RaTeXExampleContent()
        RaTeXShowcasePage()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RaTeXExampleContent() {
    var latex by rememberSaveable { mutableStateOf(sampleFormulas.first().latex) }
    var displayMode by rememberSaveable { mutableStateOf(sampleFormulas.first().displayMode) }
    var fontSizeDp by rememberSaveable { mutableFloatStateOf(28f) }
//    var color by remember { mutableStateOf(Color.Black) }
    val color = MaterialTheme.colorScheme.onSurface

    val parseResult by rememberRaTeXDisplayList(
        latex = latex,
        displayMode = displayMode,
        color = color
    )
    val fontSizePx = with(LocalDensity.current) { fontSizeDp.sp.toPx() }
    val measuredDisplayList = parseResult?.getOrNull()?.measure(fontSizePx)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RaTeX Compose Example") },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "intro") {
                IntroCard()
            }
            item(key = "editor") {
                EditorCard(
                    latex = latex,
                    displayMode = displayMode,
                    fontSizeDp = fontSizeDp,
                    onLatexChange = { latex = it },
                    onDisplayModeChange = { displayMode = it },
                    onFontSizeChange = { fontSizeDp = it },
                )
            }
            stickyHeader(key = "preview") {
                PreviewCard(
                    displayList = parseResult?.getOrNull(),
                    fontSizeDp = fontSizeDp,
                    measuredSummary = measuredDisplayList?.let {
                        "parsed ${parseResult?.getOrNull()?.items?.size ?: 0} items, " +
                                "width ${it.widthPx.toInt()}px, height ${it.totalHeightPx.toInt()}px"
                    },
                    parseError = parseResult?.exceptionOrNull()?.message
                )
            }
            item(key = "examples-header") {
                Text(
                    text = "Examples",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(
                items = sampleFormulas,
                key = { sample -> sample.id },
            ) { sample ->
                SampleFormulaCard(
                    sample = sample,
                    selected = sample.latex == latex && sample.displayMode == displayMode,
                    onClick = {
                        latex = sample.latex
                        displayMode = sample.displayMode
//                        color = sample.color
                    },
                )
            }
        }
    }
}

@Composable
private fun IntroCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Shared state, Compose-native Android rendering",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "This example module uses the Compose Multiplatform parser from :library " +
                        "and renders Android formulas through a Compose Canvas renderer inside :library.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun EditorCard(
    latex: String,
    displayMode: Boolean,
    fontSizeDp: Float,
    onLatexChange: (String) -> Unit,
    onDisplayModeChange: (Boolean) -> Unit,
    onFontSizeChange: (Float) -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Edit formula",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedTextField(
                value = latex,
                onValueChange = onLatexChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                label = { Text("LaTeX") },
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Display mode",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Switch(
                        checked = displayMode,
                        onCheckedChange = onDisplayModeChange,
                    )
                }
                Text(
                    text = if (displayMode) "Block formula" else "Inline formula",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Font size ${fontSizeDp.toInt()}dp",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Slider(
                    value = fontSizeDp,
                    onValueChange = onFontSizeChange,
                    valueRange = 16f..56f,
                )
            }
        }
    }
}

@Composable
private fun PreviewCard(
    displayList: DisplayList?,
    fontSizeDp: Float,
    measuredSummary: String?,
    parseError: String?,
) {
    val horizontalScrollState = rememberScrollState()

    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 96.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(20.dp),
                    )
                    .padding(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outline)
                        .horizontalScroll(horizontalScrollState),
                ) {
                    RaTeX(
                        displayList = displayList,
                        modifier = Modifier,
                        fontSize = fontSizeDp.sp,
                    )
                }
            }
            when {
                parseError != null -> {
                    Text(
                        text = parseError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                measuredSummary != null -> {
                    Text(
                        text = measuredSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SampleFormulaCard(
    sample: FormulaSample,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val chipLabel = remember(sample.displayMode) {
        if (sample.displayMode) "display" else "inline"
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = sample.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            FilterChip(
                selected = selected,
                onClick = onClick,
                label = { Text(chipLabel) },
            )
            Text(
                text = sample.latex,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
