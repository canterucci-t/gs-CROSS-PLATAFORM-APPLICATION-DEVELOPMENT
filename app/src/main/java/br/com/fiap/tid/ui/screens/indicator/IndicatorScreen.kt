package br.com.fiap.tid.ui.screens.indicator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.tid.appContainer
import br.com.fiap.tid.ui.common.asPercent
import br.com.fiap.tid.ui.components.AlertCard
import br.com.fiap.tid.ui.components.InfoBanner
import br.com.fiap.tid.ui.components.JustificationPanel
import br.com.fiap.tid.ui.components.KpiCard
import br.com.fiap.tid.ui.components.StateView
import br.com.fiap.tid.ui.components.TrendChart
import br.com.fiap.tid.ui.theme.TidTheme

/**
 * TELA 2/5 - DETALHE DO INDICADOR.
 * Grafico com historico e projecao, KPI, fonte dos dados e, quando existe,
 * o alerta com a justificativa.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndicatorScreen(
    territoryId: String,
    indicatorId: String,
    onBack: () -> Unit,
    onOpenAlert: (territoryId: String, indicatorId: String) -> Unit
) {
    val container = LocalContext.current.appContainer()
    val viewModel: IndicatorViewModel = viewModel(
        factory = IndicatorViewModel.factory(container),
        key = "indicator-$territoryId-$indicatorId"
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val spacing = TidTheme.spacing

    LaunchedEffect(territoryId, indicatorId) { viewModel.load(territoryId, indicatorId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalhe do indicador") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar ao painel"
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
                    "${detail.indicator.theme.label} - ${detail.indicator.name}",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    "${detail.territory.name} (${detail.territory.type.label})",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(detail.indicator.description, style = MaterialTheme.typography.bodyMedium)

                KpiCard(
                    title = "Leitura mais recente",
                    currentValue = detail.kpi.currentValue,
                    unit = detail.indicator.unit,
                    variationPercent = detail.kpi.weeklyVariationPercent,
                    level = detail.kpi.level,
                    lastReadingAt = detail.kpi.lastReadingAt,
                    originLabel = detail.kpi.origin.label,
                    onClick = { }
                )

                Text("Historico e projecao", style = MaterialTheme.typography.titleMedium)
                TrendChart(
                    points = detail.series,
                    unit = detail.indicator.unit,
                    attentionThreshold = detail.indicator.attentionThreshold,
                    criticalThreshold = detail.indicator.criticalThreshold,
                    modifier = Modifier.fillMaxWidth()
                )

                InfoBanner(
                    "${detail.readingCount} leituras locais - ${detail.kpi.origin.label}. " +
                        "Base documentada em tools/gerar_dados_sinteticos.py."
                )

                val alert = detail.alert
                if (alert != null) {
                    Text("Alerta preditivo", style = MaterialTheme.typography.titleMedium)
                    AlertCard(
                        alert = alert,
                        onClick = { onOpenAlert(territoryId, indicatorId) }
                    )
                    JustificationPanel(
                        justification = alert.justification,
                        confidenceLabel = "${alert.confidence.asPercent()} " +
                            "(derivada da aderencia do ajuste e do tamanho do historico)"
                    )
                } else {
                    Text("Sem alerta preditivo", style = MaterialTheme.typography.titleMedium)
                    InfoBanner(
                        detail.silencedReason
                            ?: "Projecao abaixo do limiar de atencao."
                    )
                }
            }
        }
    }
}
