package io.ratex.compose.example

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ratex.DisplayList
import io.ratex.FallbackGlyphMetrics
import io.ratex.RaTeXEngine
import io.ratex.RaTeXFallbackMetrics
import io.ratex.compose.RaTeX
import io.ratex.compose.rememberRaTeXDisplayList
import io.ratex.parseWithFallbackMetrics
import kotlinx.coroutines.CancellationException
import kotlin.math.roundToInt

private val illustrativeMetrics = RaTeXFallbackMetrics.of(mapOf(
    '中'.code to FallbackGlyphMetrics(widthEm = 1.15, heightEm = 0.8, depthEm = 0.12),
    '文'.code to FallbackGlyphMetrics(widthEm = 1.15, heightEm = 0.8, depthEm = 0.12),
))

@Composable
internal fun FallbackMetricsExample(latex: String) {
    val fontSize = 24.sp
    val color = LocalContentColor.current
    val original by rememberRaTeXDisplayList(latex)
    val result by produceState<Result<DisplayList>?>(null, latex, color) {
        value = null
        try {
            value = Result.success(RaTeXEngine.parseWithFallbackMetrics(latex, illustrativeMetrics, color = color))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            value = Result.failure(error)
        }
    }
    Column {
        Text("Default layout", style = MaterialTheme.typography.labelMedium)
        original?.getOrNull()?.let {
            RaTeX(displayList = it, fontSize = fontSize, modifier = Modifier.horizontalScroll(rememberScrollState()))
            MetricsLabel(it)
        }
        Spacer(Modifier.height(12.dp))
        Text("Explicit metrics · 1.15 em advance", style = MaterialTheme.typography.labelMedium)
        Text("Illustrative values. Supply metrics from the font used to draw the text.",
            style = MaterialTheme.typography.bodySmall)
        result?.getOrNull()?.let {
            RaTeX(displayList = it, fontSize = fontSize, modifier = Modifier.horizontalScroll(rememberScrollState()))
            MetricsLabel(it)
        }
        result?.exceptionOrNull()?.let { Text(it.message ?: "Unable to lay out the formula", color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun MetricsLabel(displayList: DisplayList) {
    fun rounded(value: Double): Double = (value * 1000).roundToInt() / 1000.0
    Text("Width ${rounded(displayList.width)} em · height ${rounded(displayList.height)} em · depth ${rounded(displayList.depth)} em",
        style = MaterialTheme.typography.bodySmall)
}
