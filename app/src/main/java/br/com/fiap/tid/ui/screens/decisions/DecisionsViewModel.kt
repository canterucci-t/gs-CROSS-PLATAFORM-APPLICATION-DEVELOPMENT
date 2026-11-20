package br.com.fiap.tid.ui.screens.decisions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.fiap.tid.AppContainer
import br.com.fiap.tid.domain.model.Decision
import br.com.fiap.tid.domain.repository.TerritoryRepository
import br.com.fiap.tid.ui.common.UiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * O historico vem direto do Room como Flow: o Room notifica a mudanca, o
 * StateFlow atualiza e a tela recompoe. Nenhum "refresh" manual e necessario.
 */
class DecisionsViewModel(
    private val repository: TerritoryRepository
) : ViewModel() {

    val state: StateFlow<UiState<List<Decision>>> = repository.observeDecisions()
        .map { list ->
            if (list.isEmpty()) {
                UiState.Empty(
                    "Nenhuma decisao registrada ainda. Abra um alerta no painel e " +
                        "registre acatar, adiar ou descartar."
                )
            } else {
                UiState.Success(list)
            }
        }
        .catch { e ->
            emit(UiState.Error("Falha ao ler o historico: ${e.message ?: "erro desconhecido"}."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading
        )

    fun clearAll() {
        viewModelScope.launch { repository.clearDecisions() }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { DecisionsViewModel(container.repository) }
        }
    }
}
