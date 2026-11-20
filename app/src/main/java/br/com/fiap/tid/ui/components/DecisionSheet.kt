package br.com.fiap.tid.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import br.com.fiap.tid.domain.model.Alert
import br.com.fiap.tid.domain.model.DecisionAction
import br.com.fiap.tid.ui.common.asPercent
import br.com.fiap.tid.ui.theme.TidTheme

/**
 * COMPONENTE REUTILIZAVEL 7/7 - DecisionSheet.
 *
 * Registro da DECISAO HUMANA sobre um alerta: acatar, adiar ou descartar,
 * com responsavel e observacao. O app recomenda; a pessoa decide.
 *
 * O rascunho do formulario usa rememberSaveable para sobreviver a rotacao.
 * A validacao de negocio NAO esta aqui - fica no RegisterDecisionUseCase.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DecisionSheet(
    alert: Alert,
    defaultResponsible: String,
    errorMessage: String?,
    onEdit: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (responsible: String, action: DecisionAction, note: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val spacing = TidTheme.spacing
    var responsible by rememberSaveable { mutableStateOf(defaultResponsible) }
    var note by rememberSaveable { mutableStateOf("") }
    var action by rememberSaveable { mutableStateOf(DecisionAction.ACATAR) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = spacing.md, end = spacing.md, bottom = spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Text("Registrar decisao humana", style = MaterialTheme.typography.titleLarge)
            Text(alert.title, style = MaterialTheme.typography.bodyLarge)
            Text(
                "Recomendacao do app: ${alert.recommendation}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Confianca do alerta: ${alert.confidence.asPercent()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (alert.requiresHumanConfirmation) {
                WarningBanner(
                    "Alerta critico: a acao sugerida tem alto impacto. Nada e executado " +
                        "pelo app - a decisao e sua e fica registrada com seu nome."
                )
            }

            Text("Acao", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                DecisionAction.entries.forEach { option ->
                    FilterChip(
                        selected = action == option,
                        onClick = { action = option },
                        label = { Text(option.label) },
                        modifier = Modifier.defaultMinSize(minHeight = spacing.touchTarget)
                    )
                }
            }

            OutlinedTextField(
                value = responsible,
                onValueChange = { responsible = it; onEdit() },
                label = { Text("Responsavel pela decisao") },
                supportingText = { Text("Fica registrado no historico (rastreabilidade).") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it; onEdit() },
                label = { Text("Observacao") },
                supportingText = {
                    Text(
                        if (alert.requiresHumanConfirmation) {
                            "Obrigatoria para alerta critico."
                        } else {
                            "Opcional."
                        }
                    )
                },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Text(
                    errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = { onConfirm(responsible, action, note) },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = spacing.touchTarget)
            ) {
                Text("Confirmar decisao")
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = spacing.touchTarget)
            ) {
                Text("Cancelar")
            }
        }
    }
}
