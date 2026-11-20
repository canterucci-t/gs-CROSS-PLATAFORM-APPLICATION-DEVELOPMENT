package br.com.fiap.tid.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.tid.appContainer
import br.com.fiap.tid.domain.model.Theme
import br.com.fiap.tid.domain.repository.DataFailureMode
import br.com.fiap.tid.ui.common.format1
import br.com.fiap.tid.ui.components.InfoBanner
import br.com.fiap.tid.ui.components.StateView
import br.com.fiap.tid.ui.components.TerritorySelector
import br.com.fiap.tid.ui.components.WarningBanner
import br.com.fiap.tid.ui.theme.TidTheme

/**
 * TELA 5/5 - CONFIGURACOES.
 * Territorio, temas monitorados, limiares, permissao de notificacao,
 * origem/licenca dos dados, LGPD e os cenarios de falha para demonstracao.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val container = context.appContainer()
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val spacing = TidTheme.spacing

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuracoes") },
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
            onRetry = viewModel::load,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { data ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                Text("Territorio", style = MaterialTheme.typography.titleLarge)
                TerritorySelector(
                    territories = data.territories,
                    selected = data.selected,
                    onSelect = viewModel::selectTerritory
                )

                HorizontalDivider()
                Text("Temas monitorados", style = MaterialTheme.typography.titleLarge)
                Theme.entries.forEach { theme ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = spacing.touchTarget),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(theme.label, style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = theme in data.monitoredThemes,
                            onCheckedChange = { viewModel.toggleTheme(theme, it) }
                        )
                    }
                }

                HorizontalDivider()
                Text(
                    "Limiares de referencia",
                    style = MaterialTheme.typography.titleLarge
                )
                data.indicators.forEach { indicator ->
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            "${indicator.theme.label} - ${indicator.name}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Atencao a partir de " +
                                "${indicator.attentionThreshold.format1()} ${indicator.unit}; " +
                                "critico a partir de " +
                                "${indicator.criticalThreshold.format1()} ${indicator.unit}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider()
                Text("Notificacoes", style = MaterialTheme.typography.titleLarge)
                if (data.notificationsAllowed) {
                    InfoBanner(
                        "Permissao concedida: alertas criticos podem gerar notificacao local."
                    )
                } else {
                    WarningBanner(
                        "Permissao de notificacao negada. Plano B ativo: os alertas " +
                            "continuam visiveis no painel. Use o botao abaixo para " +
                            "liberar a permissao nas configuracoes do sistema."
                    )
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                            runCatching { context.startActivity(intent) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = spacing.touchTarget)
                    ) {
                        Text("Abrir as configuracoes do sistema")
                    }
                }

                HorizontalDivider()
                Text("Fontes de dados", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Base 100% SINTETICA, gerada por tools/gerar_dados_sinteticos.py " +
                        "com semente fixa (2026). Calibrada na ordem de grandeza de " +
                        "publicacoes abertas (ONS, SNIS e INMET), sem reproduzir nenhuma " +
                        "serie real. Licenca: uso academico livre (GS2026.2). " +
                        "Nenhum dado pessoal e coletado, armazenado ou transmitido.",
                    style = MaterialTheme.typography.bodyMedium
                )

                HorizontalDivider()
                Text(
                    "Privacidade (LGPD)",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    "O app funciona offline e guarda apenas no proprio aparelho: " +
                        "as leituras sinteticas, o territorio escolhido e o historico de " +
                        "decisoes (nome do responsavel, acao, observacao e data). " +
                        "Nada e enviado para servidores. Voce pode apagar o historico " +
                        "a qualquer momento.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(
                    onClick = viewModel::clearDecisions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = spacing.touchTarget)
                ) {
                    Text("Apagar o historico de decisoes")
                }

                HorizontalDivider()
                Text(
                    "Simulacao de falha (demonstracao)",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    "Use estas opcoes para ver como o app degrada quando os dados " +
                        "faltam, vencem ou a leitura falha.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                DataFailureMode.entries.forEach { mode ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = spacing.touchTarget)
                            .selectable(
                                selected = data.failureMode == mode,
                                onClick = { viewModel.setFailureMode(mode) }
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                    ) {
                        RadioButton(
                            selected = data.failureMode == mode,
                            onClick = { viewModel.setFailureMode(mode) }
                        )
                        Column(Modifier.weight(1f)) {
                            Text(mode.label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                mode.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider()
                Text("Sobre", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Territory Intelligence Dashboard 1.0.0 - GS2026.2 FIAP\n" +
                        "Cross-Platform Application Development\n" +
                        "Android nativo com Kotlin e Jetpack Compose\n" +
                        "minSdk 24 - targetSdk 36",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
