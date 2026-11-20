package br.com.fiap.tid.ui.screens.alert

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.tid.appContainer
import br.com.fiap.tid.ui.common.asPercent
import br.com.fiap.tid.ui.components.AlertCard
import br.com.fiap.tid.ui.components.DecisionSheet
import br.com.fiap.tid.ui.components.InfoBanner
import br.com.fiap.tid.ui.components.JustificationPanel
import br.com.fiap.tid.ui.components.StateView
import br.com.fiap.tid.ui.components.TrendChart
import br.com.fiap.tid.ui.components.WarningBanner
import br.com.fiap.tid.ui.theme.TidTheme

/**
 * TELA 3/5 - DETALHE DO ALERTA.
 * Justificativa completa, confianca, fonte e registro da decisao humana.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertScreen(
    territoryId: String,
    indicatorId: String,
    onBack: () -> Unit
) {
    val container = LocalContext.current.appContainer()
    val viewModel: AlertViewModel = viewModel(
        factory = AlertViewModel.factory(container),
        key = "alert-$territoryId-$indicatorId"
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetVisible by viewModel.sheetVisible.collectAsStateWithLifecycle()
    val formError by viewModel.formError.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val spacing = TidTheme.spacing

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.notifyThisAlert() }

    LaunchedEffect(territoryId, indicatorId) { viewModel.load(territoryId, indicatorId) }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Detalhe do alerta") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                }
            )
        }
    ) { padding ->
        StateView(
            state = state,
            onRetry = { viewModel.load(territoryId, indicatorId) },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { detail ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                Text(
                    "${detail.territory.name} - ${detail.indicator.name}",
                    style = MaterialTheme.typography.titleLarge
                )

                val alert = detail.alert
                if (alert == null) {
                    InfoBanner(
                        detail.silencedReason
                            ?: "Nenhum alerta ativo para este indicador."
                    )
                    TrendChart(
                        points = detail.series,
                        unit = detail.indicator.unit,
                        attentionThreshold = detail.indicator.attentionThreshold,
                        criticalThreshold = detail.indicator.criticalThreshold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    return@StateView
                }

                AlertCard(alert = alert, onClick = { })

                JustificationPanel(
                    justification = alert.justification,
                    confidenceLabel = "${alert.confidence.asPercent()} - " +
                        "quanto maior a aderencia da tendencia, maior a confianca. " +
                        "Abaixo de 20% de aderencia o app nao emite alerta."
                )

                Text("Historico e projecao", style = MaterialTheme.typography.titleMedium)
                TrendChart(
                    points = detail.series,
                    unit = detail.indicator.unit,
                    attentionThreshold = detail.indicator.attentionThreshold,
                    criticalThreshold = detail.indicator.criticalThreshold,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("O que o app recomenda", style = MaterialTheme.typography.titleMedium)
                Text(alert.recommendation, style = MaterialTheme.typography.bodyLarge)

                if (alert.requiresHumanConfirmation) {
                    WarningBanner(
                        "Humano no circuito: medidas de alto impacto (racionamento, " +
                            "interrupcao de servico, evacuacao) nunca sao executadas pelo " +
                            "app. Ele apenas recomenda e registra a sua decisao."
                    )
                }

                Button(
                    onClick = viewModel::openSheet,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = spacing.touchTarget)
                ) {
                    Text("Registrar decisao humana")
                }

                OutlinedButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            viewModel.notifierNeedsPermission()
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.notifyThisAlert()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = spacing.touchTarget)
                ) {
                    Text("Notificar a equipe (notificacao local)")
                }

                Text(
                    "A notificacao local e um recurso nativo do dispositivo. Se a " +
                        "permissao for negada, o app avisa na tela e mantem o alerta " +
                        "visivel no painel - nenhum alerta se perde.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (sheetVisible) {
                    DecisionSheet(
                        alert = alert,
                        defaultResponsible = viewModel.defaultResponsible,
                        errorMessage = formError,
                        onEdit = viewModel::clearFormError,
                        onDismiss = viewModel::closeSheet,
                        onConfirm = viewModel::confirmDecision
                    )
                }
            }
        }
    }
}
