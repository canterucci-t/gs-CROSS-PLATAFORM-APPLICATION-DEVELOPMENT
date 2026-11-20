package br.com.fiap.tid.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import br.com.fiap.tid.domain.model.AlertLevel
import br.com.fiap.tid.ui.theme.TidTheme

/**
 * Traducao de um nivel de alerta para cor, icone e rotulo.
 *
 * Regra de acessibilidade do projeto: a cor nunca comunica o nivel sozinha -
 * sempre acompanha [icon] e [label] em texto.
 */
data class LevelVisual(
    val container: Color,
    val content: Color,
    val icon: ImageVector,
    val label: String
)

@Composable
fun levelVisual(level: AlertLevel?): LevelVisual {
    val status = TidTheme.status
    return when (level) {
        AlertLevel.CRITICO -> LevelVisual(
            status.critical, status.onCritical, Icons.Filled.Warning, "Critico"
        )
        AlertLevel.ATENCAO -> LevelVisual(
            status.attention, status.onAttention, Icons.Filled.Warning, "Atencao"
        )
        AlertLevel.INFORMATIVO -> LevelVisual(
            status.info, status.onInfo, Icons.Filled.Info, "Informativo"
        )
        null -> LevelVisual(
            status.normal, status.onNormal, Icons.Filled.CheckCircle, "Dentro do normal"
        )
    }
}
