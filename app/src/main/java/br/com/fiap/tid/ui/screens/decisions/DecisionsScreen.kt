package br.com.fiap.tid.ui.screens.decisions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.tid.appContainer
import br.com.fiap.tid.ui.common.asDateTime
import br.com.fiap.tid.ui.components.InfoBanner
import br.com.fiap.tid.ui.components.StateView
import br.com.fiap.tid.ui.theme.TidTheme

/**
 * TELA 4/5 - HISTORICO DE DECISOES.
 * Rastreabilidade: cada decisao guarda data, responsavel, acao e observacao.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecisionsScreen(onBack: () -> Unit) {
    val container = LocalContext.current.appContainer()
    val viewModel: DecisionsViewModel = viewModel(
        factory = DecisionsViewModel.factory(container)
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val spacing = TidTheme.spacing

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historico de decisoes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar ao painel"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::clearAll) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Apagar o historico de decisoes (LGPD)"
                        )
                    }
                }
            )
        }
    ) { padding ->
        StateView(
            state = state,
            onRetry = { },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { decisions ->
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    vertical = spacing.md
                )
            ) {
                item {
                    InfoBanner(
                        "${decisions.size} decisao(oes) registrada(s) neste dispositivo. " +
                            "Nenhum dado pessoal sai do aparelho; o botao da lixeira apaga " +
                            "todo o historico (LGPD)."
                    )
                }
                items(decisions, key = { it.id }) { decision ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(spacing.md),
                            verticalArrangement = Arrangement.spacedBy(spacing.xs)
                        ) {
                            Text(
                                decision.action.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                decision.alertTitle,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                "Territorio: ${decision.territoryName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Responsavel: ${decision.responsible}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (decision.note.isNotBlank()) {
                                Text(
                                    "Observacao: ${decision.note}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Text(
                                decision.createdAt.asDateTime(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
