package br.com.fiap.tid.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.fiap.tid.AppContainer
import br.com.fiap.tid.domain.model.Indicator
import br.com.fiap.tid.domain.model.Territory
import br.com.fiap.tid.domain.model.Theme
import br.com.fiap.tid.domain.platform.AlertNotifier
import br.com.fiap.tid.domain.repository.DataFailureMode
import br.com.fiap.tid.domain.repository.TerritoryRepository
import br.com.fiap.tid.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsData(
    val territories: List<Territory>,
    val selected: Territory,
    val indicators: List<Indicator>,
    val monitoredThemes: Set<Theme>,
    val failureMode: DataFailureMode,
    val notificationsAllowed: Boolean
)

class SettingsViewModel(
    private val repository: TerritoryRepository,
    private val notifier: AlertNotifier
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<SettingsData>>(UiState.Loading)
    val state: StateFlow<UiState<SettingsData>> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = runCatching {
                repository.ensureSeeded()
                val territories = repository.territories()
                val selectedId = repository.selectedTerritoryId()
                SettingsData(
                    territories = territories,
                    selected = territories.firstOrNull { it.id == selectedId }
                        ?: territories.first(),
                    indicators = repository.indicators(),
                    monitoredThemes = repository.monitoredThemes()
                        .mapNotNull { name -> Theme.entries.firstOrNull { it.name == name } }
                        .toSet(),
                    failureMode = repository.dataFailureMode(),
                    notificationsAllowed = notifier.hasPermission()
                )
            }.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { e ->
                    UiState.Error(
                        "Falha ao abrir as configuracoes: ${e.message ?: "erro desconhecido"}."
                    )
                }
            )
        }
    }

    fun selectTerritory(territory: Territory) {
        viewModelScope.launch {
            repository.selectTerritory(territory.id)
            load()
        }
    }

    fun toggleTheme(theme: Theme, monitored: Boolean) {
        viewModelScope.launch {
            repository.setThemeMonitored(theme.name, monitored)
            load()
        }
    }

    fun setFailureMode(mode: DataFailureMode) {
        viewModelScope.launch {
            repository.setDataFailureMode(mode)
            load()
        }
    }

    fun clearDecisions() {
        viewModelScope.launch {
            repository.clearDecisions()
            load()
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(container.repository, container.notifier) }
        }
    }
}
