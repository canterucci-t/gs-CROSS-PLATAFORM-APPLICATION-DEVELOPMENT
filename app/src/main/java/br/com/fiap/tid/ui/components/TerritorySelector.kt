package br.com.fiap.tid.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.com.fiap.tid.domain.model.Territory
import br.com.fiap.tid.ui.theme.TidTheme

/**
 * COMPONENTE REUTILIZAVEL 5/7 - TerritorySelector.
 *
 * Usado no Painel e em Configuracoes. O estado "menu aberto" e LOCAL do
 * componente (vive na tela); o territorio selecionado e estado compartilhado
 * e sobe por callback - elevacao de estado.
 */
@Composable
fun TerritorySelector(
    territories: List<Territory>,
    selected: Territory,
    onSelect: (Territory) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val spacing = TidTheme.spacing

    Column(modifier) {
        Surface(
            onClick = { expanded = true },
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = spacing.touchTarget)
                .semantics {
                    contentDescription =
                        "Territorio selecionado: ${selected.name}, ${selected.type.label}. " +
                            "Toque para trocar de territorio."
                }
        ) {
            Row(
                Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(22.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Territorio monitorado",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        "${selected.name} - ${selected.type.label}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            territories.forEach { territory ->
                DropdownMenuItem(
                    text = {
                        Text(
                            "${territory.name} - ${territory.type.label}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelect(territory)
                    },
                    modifier = Modifier.defaultMinSize(minHeight = spacing.touchTarget)
                )
            }
        }
    }
}
