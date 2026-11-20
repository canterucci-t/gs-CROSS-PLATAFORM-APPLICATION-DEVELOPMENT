package br.com.fiap.tid.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.fiap.tid.domain.model.SeriesPoint
import br.com.fiap.tid.ui.common.asShortDateTime
import br.com.fiap.tid.ui.common.format1
import br.com.fiap.tid.ui.theme.TidTheme

/**
 * COMPONENTE REUTILIZAVEL 2/7 - TrendChart.
 *
 * Grafico de serie temporal com faixa de projecao (tracejada) e linhas de
 * limiar. Desenhado em Canvas para nao adicionar dependencia de grafico.
 * Sem regra de negocio: recebe os pontos ja calculados pelo dominio.
 *
 * Acessibilidade: o Canvas e opaco para o TalkBack, por isso o grafico expoe
 * um resumo textual em [contentDescription] e repete os valores-chave na
 * legenda abaixo.
 */
@Composable
fun TrendChart(
    points: List<SeriesPoint>,
    unit: String,
    attentionThreshold: Double,
    criticalThreshold: Double,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp
) {
    val spacing = TidTheme.spacing
    val status = TidTheme.status
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    if (points.size < 2) {
        Text(
            text = "Pontos insuficientes para desenhar o grafico.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
        )
        return
    }

    val history = points.filter { !it.isForecast }
    val forecast = points.filter { it.isForecast }
    val minValue = points.minOf { it.value }
    val maxValue = maxOf(points.maxOf { it.value }, attentionThreshold)
    val lowerBound = minOf(minValue, attentionThreshold * 0.6)
    val span = (maxValue - lowerBound).takeIf { it > 1e-6 } ?: 1.0

    val summary = buildString {
        append("Grafico de tendencia. ")
        append("${history.size} leituras do historico, de ")
        append("${history.first().value.format1()} a ${history.last().value.format1()} $unit. ")
        if (forecast.isNotEmpty()) {
            append(
                "Projecao horaria de curto prazo ate " +
                    "${forecast.last().value.format1()} $unit. "
            )
        }
        append("Limiar de atencao ${attentionThreshold.format1()} $unit, ")
        append("limiar critico ${criticalThreshold.format1()} $unit.")
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
                .clearAndSetSemantics { contentDescription = summary }
        ) {
            val w = size.width
            val h = size.height
            fun y(value: Double): Float =
                (h - ((value - lowerBound) / span * h)).toFloat().coerceIn(0f, h)
            fun x(index: Int): Float = w * index / (points.size - 1).toFloat()

            // Linhas de limiar (horizontais, com tracejado curto)
            val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            drawLine(
                color = status.threshold.copy(alpha = 0.8f),
                start = Offset(0f, y(criticalThreshold)),
                end = Offset(w, y(criticalThreshold)),
                strokeWidth = 2f,
                pathEffect = dash
            )
            drawLine(
                color = gridColor,
                start = Offset(0f, y(attentionThreshold)),
                end = Offset(w, y(attentionThreshold)),
                strokeWidth = 2f,
                pathEffect = dash
            )

            // Historico
            val historyPath = Path()
            history.forEachIndexed { i, p ->
                val px = x(i)
                val py = y(p.value)
                if (i == 0) historyPath.moveTo(px, py) else historyPath.lineTo(px, py)
            }
            drawPath(historyPath, color = lineColor, style = Stroke(width = 4f))

            // Projecao: continua do ultimo ponto do historico, tracejada
            if (forecast.isNotEmpty()) {
                // A projecao e desenhada separada do historico: ela descreve a
                // LINHA DE TENDENCIA, nao a continuacao da ultima leitura bruta.
                // Liga-las criaria um degrau que nao existe no dado.
                val forecastPath = Path()
                forecast.forEachIndexed { i, p ->
                    val px = x(history.size + i)
                    val py = y(p.value)
                    if (i == 0) forecastPath.moveTo(px, py) else forecastPath.lineTo(px, py)
                }
                drawPath(
                    forecastPath,
                    color = status.forecast,
                    style = Stroke(width = 4f, pathEffect = dash)
                )
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                points.first().timestamp.asShortDateTime(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                points.last().timestamp.asShortDateTime(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Legenda textual - o grafico nunca depende apenas da cor
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            LegendItem(lineColor, "Historico (${history.size} leituras)")
            if (forecast.isNotEmpty()) {
                LegendItem(
                    status.forecast,
                    "Projecao horaria de curto prazo (tracejada): " +
                        "${forecast.last().value.format1()} $unit"
                )
            }
            LegendItem(
                status.threshold,
                "Limiar critico: ${criticalThreshold.format1()} $unit"
            )
            LegendItem(
                MaterialTheme.colorScheme.outline,
                "Limiar de atencao: ${attentionThreshold.format1()} $unit"
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(color = color, shape = CircleShape, modifier = Modifier.size(10.dp)) {}
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
