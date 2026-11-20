package br.com.fiap.tid.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import br.com.fiap.tid.domain.model.Justification
import br.com.fiap.tid.ui.common.asDateTime
import br.com.fiap.tid.ui.common.asRelativeTime
import br.com.fiap.tid.ui.theme.TidTheme

/**
 * COMPONENTE REUTILIZAVEL 4/7 - JustificationPanel.
 *
 * Exibe POR QUE o alerta foi emitido: variaveis e valores, a regra aplicada,
 * o periodo analisado, a fonte do dado e quando foi atualizado.
 * Usado no Detalhe do Alerta e no Detalhe do Indicador.
 */
@Composable
fun JustificationPanel(
    justification: Justification,
    confidenceLabel: String,
    modifier: Modifier = Modifier
) {
    val spacing = TidTheme.spacing
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Text(
                "Por que este alerta existe",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            justification.variables.forEach { (name, value) ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(name, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            HorizontalDivider()
            LabeledBlock("Regra aplicada", justification.rule)
            LabeledBlock("Periodo analisado", justification.period)
            LabeledBlock("Confianca", confidenceLabel)
            LabeledBlock("Origem do dado", justification.source)
            LabeledBlock(
                "Ultima atualizacao",
                "${justification.updatedAt.asDateTime()} " +
                    "(${justification.updatedAt.asRelativeTime()})"
            )
        }
    }
}

@Composable
private fun LabeledBlock(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
