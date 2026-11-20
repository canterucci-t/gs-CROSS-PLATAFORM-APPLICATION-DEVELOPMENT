package br.com.fiap.tid.ui.screens.indicator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.fiap.tid.AppContainer
import br.com.fiap.tid.domain.usecase.GetIndicatorDetailUseCase
import br.com.fiap.tid.domain.usecase.IndicatorDetail
import br.com.fiap.tid.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IndicatorViewModel(
    private val getIndicatorDetail: GetIndicatorDetailUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<IndicatorDetail>>(UiState.Loading)
    val state: StateFlow<UiState<IndicatorDetail>> = _state.asStateFlow()

    private var lastKey: String? = null

    fun load(territoryId: String, indicatorId: String) {
        val key = "$territoryId|$indicatorId"
        // Evita carregamento duplicado na recomposicao e na volta da pilha.
        if (lastKey == key && _state.value is UiState.Success) return
        lastKey = key
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = runCatching { getIndicatorDetail(territoryId, indicatorId) }.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { e ->
                    UiState.Error(
                        "Nao foi possivel carregar o indicador: " +
                            "${e.message ?: "erro desconhecido"}."
                    )
                }
            )
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { IndicatorViewModel(container.getIndicatorDetail) }
        }
    }
}
