package br.com.fiap.tid.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.fiap.tid.ui.common.asRelativeTime
import br.com.fiap.tid.ui.common.asSignedPercent
import br.com.fiap.tid.ui.common.format1
import br.com.fiap.tid.ui.theme.TidTheme

/**
 * COMPONENTE REUTILIZAVEL 1/7 - KpiCard.
 *
 * Usado no Painel e no Detalhe do Indicador. Recebe tudo por parametro e
 * devolve o toque por callback: o componente DESCREVE, nao decide.
 */
@Composable
fun KpiCard(
    title: String,
    value: String,
    unit: String,
    variationPercent: Double,
    levelLabel: String,
    levelContainer: androidx.compose.ui.graphics.Color,
    levelContent: androidx.compose.ui.graphics.Color,
    levelIcon: androidx.compose.ui.graphics.vector.ImageVector,
    lastReadingAt: Long,
    originLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = TidTheme.spacing
    val description = "$title: $value $unit, $levelLabel, " +
        "variacao de ${variationPercent.asSignedPercent()} na semana, " +
        "atualizado ${lastReadingAt.asRelativeTime()}, $originLabel."

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = spacing.touchTarget)
            .clearAndSetSemantics { contentDescription = description },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                StatusChip(levelLabel, levelContainer, levelContent, levelIcon)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.headlineMedium)
                Text(
                    " $unit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Text(
                text = "${variationPercent.asSignedPercent()} na semana  -  " +
                    "atualizado ${lastReadingAt.asRelativeTime()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = originLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Selo de status: cor + icone + rotulo (a cor nunca informa sozinha). */
@Composable
fun StatusChip(
    label: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.defaultMinSize(16.dp, 16.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Sobrecarga de conveniencia usada pelas telas. */
@Composable
fun KpiCard(
    title: String,
    currentValue: Double,
    unit: String,
    variationPercent: Double,
    level: br.com.fiap.tid.domain.model.AlertLevel?,
    lastReadingAt: Long,
    originLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visual = levelVisual(level)
    KpiCard(
        title = title,
        value = currentValue.format1(),
        unit = unit,
        variationPercent = variationPercent,
        levelLabel = visual.label,
        levelContainer = visual.container,
        levelContent = visual.content,
        levelIcon = visual.icon,
        lastReadingAt = lastReadingAt,
        originLabel = originLabel,
        onClick = onClick,
        modifier = modifier
    )
}
