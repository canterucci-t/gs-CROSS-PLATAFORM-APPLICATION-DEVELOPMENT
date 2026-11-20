package br.com.fiap.tid.ui.screens.dashboard

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.tid.appContainer
import br.com.fiap.tid.ui.common.UiState
import br.com.fiap.tid.ui.common.asDateTime
import br.com.fiap.tid.ui.common.asRelativeTime
import br.com.fiap.tid.ui.components.AlertCard
import br.com.fiap.tid.ui.components.InfoBanner
import br.com.fiap.tid.ui.components.KpiCard
import br.com.fiap.tid.ui.components.StateView
import br.com.fiap.tid.ui.components.TerritorySelector
import br.com.fiap.tid.ui.components.WarningBanner
import br.com.fiap.tid.ui.theme.TidTheme

/**
 * TELA 1/5 - PAINEL.
 * Visao geral do territorio: indicadores principais, alertas ativos e o
 * motivo de cada indicador que NAO gerou alerta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenIndicator: (territoryId: String, indicatorId: String) -> Unit,
    onOpenAlert: (territoryId: String, indicatorId: String) -> Unit,
    onOpenDecisions: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val container = LocalContext.current.appContainer()
    val viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val spacing = TidTheme.spacing

    // Pedimos a permissao NO TOQUE que precisa dela, com o motivo na tela.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.notifyMostCritical() }

    // Recarrega sempre que a tela volta ao primeiro plano (inclusive ao voltar
    // de Configuracoes), para o painel nunca mostrar um estado ja superado.
    LifecycleResumeEffect(Unit) {
        viewModel.load()
        onPauseOrDispose { }
    }

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
                title = { Text("Painel territorial") },
                actions = {
                    IconButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                !container.notifier.hasPermission()
                            ) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.notifyMostCritical()
                            }
                        }
                    ) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = "Notificar o alerta mais grave"
                        )
                    }
                    IconButton(onClick = onOpenDecisions) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Historico de decisoes")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Configuracoes")
                    }
                }
            )
        }
    ) { padding ->
        StateView(
            state = state,
            onRetry = viewModel::load,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { data ->
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    top = spacing.md,
                    bottom = spacing.xl
                )
            ) {
                item {
                    TerritorySelector(
                        territories = data.territories,
                        selected = data.territory,
                        onSelect = { viewModel.selectTerritory(it.id) }
                    )
                }

                item {
                    val warning = (state as? UiState.Success)?.staleWarning
                    if (warning != null) {
                        WarningBanner(warning)
                    } else {
                        InfoBanner(
                            "${data.origin.label} - ultima atualizacao " +
                                "${data.lastUpdatedAt.asDateTime()} " +
                                "(${data.lastUpdatedAt.asRelativeTime()})."
                        )
                    }
                }

                item {
                    Text(
                        if (data.alerts.isEmpty()) {
                            "Alertas ativos (nenhum)"
                        } else {
                            "Alertas ativos (${data.alerts.size})"
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                if (data.alerts.isEmpty()) {
                    item {
                        InfoBanner(
                            if (data.isStale) {
                                "Nenhum alerta ativo em ${data.territory.name}: os alertas " +
                                    "estao SUSPENSOS porque a base local esta vencida. " +
                                    "O app prefere nao alertar sobre dado velho."
                            } else {
                                "Nenhum alerta preditivo ativo em ${data.territory.name}. " +
                                    "Os indicadores projetados seguem abaixo do limiar " +
                                    "de atencao."
                            }
                        )
                    }
                } else {
                    items(data.alerts, key = { "alerta-${it.id}" }) { alert ->
                        AlertCard(
                            alert = alert,
                            onClick = { onOpenAlert(alert.territoryId, alert.indicatorId) }
                        )
                    }
                }

                item {
                    Text("Indicadores", style = MaterialTheme.typography.titleLarge)
                }

                items(data.kpis, key = { "kpi-${it.indicator.id}" }) { kpi ->
                    KpiCard(
                        title = "${kpi.indicator.theme.label} - ${kpi.indicator.name}",
                        currentValue = kpi.currentValue,
                        unit = kpi.indicator.unit,
                        variationPercent = kpi.weeklyVariationPercent,
                        level = kpi.level,
                        lastReadingAt = kpi.lastReadingAt,
                        originLabel = kpi.origin.label,
                        onClick = { onOpenIndicator(data.territory.id, kpi.indicator.id) }
                    )
                }

                if (data.silencedReasons.isNotEmpty()) {
                    item {
                        Text(
                            "Por que estes indicadores nao geraram alerta",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    items(data.silencedReasons, key = { "silenciado-${it.first.id}" }) { (indicator, reason) ->
                        Column(Modifier.fillMaxWidth()) {
                            Text(
                                "${indicator.theme.label} - ${indicator.name}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                reason,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
