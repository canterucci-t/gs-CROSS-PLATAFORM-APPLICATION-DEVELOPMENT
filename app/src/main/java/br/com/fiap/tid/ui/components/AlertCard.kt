package br.com.fiap.tid.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import br.com.fiap.tid.domain.model.Alert
import br.com.fiap.tid.ui.common.asPercent
import br.com.fiap.tid.ui.common.asRelativeTime
import br.com.fiap.tid.ui.theme.TidTheme

/**
 * COMPONENTE REUTILIZAVEL 3/7 - AlertCard.
 *
 * Usado no Painel, no Detalhe do Indicador e no Detalhe do Alerta.
 * Mostra nivel (icone + rotulo + cor), resumo, confianca e da acesso a
 * justificativa. Nao calcula nada: recebe o [Alert] pronto do dominio.
 */
@Composable
fun AlertCard(
    alert: Alert,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showConfidence: Boolean = true
) {
    val spacing = TidTheme.spacing
    val visual = levelVisual(alert.level)
    val description = "${visual.label}. ${alert.title}. ${alert.summary} " +
        "Confianca ${alert.confidence.asPercent()}. " +
        "Fonte ${alert.justification.source}. " +
        "Atualizado ${alert.justification.updatedAt.asRelativeTime()}. " +
        "Toque para ver a justificativa completa."

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = spacing.touchTarget)
            .clearAndSetSemantics { contentDescription = description },
        colors = CardDefaults.cardColors(
            containerColor = visual.container,
            contentColor = visual.content
        )
    ) {
        Column(
            Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                Icon(visual.icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(
                    "${visual.label} - ${alert.theme.label}",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Text(alert.title, style = MaterialTheme.typography.titleMedium)
            Text(alert.summary, style = MaterialTheme.typography.bodyMedium)

            if (showConfidence) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    Text(
                        "Confianca ${alert.confidence.asPercent()}  -  " +
                            alert.justification.source,
                        style = MaterialTheme.typography.labelLarge
                    )
                    LinearProgressIndicator(
                        progress = { alert.confidence.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                        color = visual.content,
                        trackColor = visual.content.copy(alpha = 0.25f)
                    )
                }
            }

            if (alert.requiresHumanConfirmation) {
                Text(
                    "Decisao de alto impacto: exige confirmacao de um responsavel. " +
                        "O app nao executa esta acao automaticamente.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                "Toque para ver a justificativa e registrar a decisao",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
