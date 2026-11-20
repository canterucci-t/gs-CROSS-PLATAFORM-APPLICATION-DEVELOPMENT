package br.com.fiap.tid.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.fiap.tid.ui.common.UiState
import br.com.fiap.tid.ui.theme.TidTheme

/**
 * COMPONENTE REUTILIZAVEL 6/7 - StateView.
 *
 * Padroniza carregando / vazio / erro e o aviso de dados desatualizados em
 * TODAS as telas. O app nunca mostra tela branca nem erro sem saida: sempre
 * ha mensagem e, quando faz sentido, botao de tentar novamente.
 */
@Composable
fun <T> StateView(
    state: UiState<T>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    when (state) {
        is UiState.Loading -> CenteredMessage(modifier) {
            CircularProgressIndicator()
            Text("Carregando dados locais...", style = MaterialTheme.typography.bodyLarge)
        }

        is UiState.Empty -> CenteredMessage(modifier) {
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                state.message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            RetryButton(onRetry)
        }

        is UiState.Error -> CenteredMessage(modifier) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                state.message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            RetryButton(onRetry)
        }

        // O modifier (padding do Scaffold) vale para TODOS os estados,
        // inclusive o de sucesso - caso contrario o conteudo passa por
        // baixo da barra superior.
        is UiState.Success -> Box(modifier) { content(state.data) }
    }
}

/** Faixa de aviso para dado desatualizado, incompleto ou simulado. */
@Composable
fun WarningBanner(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Warning
) {
    val spacing = TidTheme.spacing
    val status = TidTheme.status
    Surface(
        color = status.attention,
        contentColor = status.onAttention,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Faixa informativa neutra (origem do dado, contexto). */
@Composable
fun InfoBanner(message: String, modifier: Modifier = Modifier) {
    val spacing = TidTheme.spacing
    val status = TidTheme.status
    Surface(
        color = status.info,
        contentColor = status.onInfo,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun RetryButton(onRetry: () -> Unit) {
    val spacing = TidTheme.spacing
    Button(
        onClick = onRetry,
        modifier = Modifier.defaultMinSize(minHeight = spacing.touchTarget)
    ) {
        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
        Text("  Tentar novamente")
    }
}

@Composable
private fun CenteredMessage(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val spacing = TidTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.md, Alignment.CenterVertically)
    ) {
        content()
    }
}
