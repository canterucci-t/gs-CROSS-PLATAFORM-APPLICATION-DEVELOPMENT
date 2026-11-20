package br.com.fiap.tid.ui.screens.alert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.fiap.tid.AppContainer
import br.com.fiap.tid.domain.model.DecisionAction
import br.com.fiap.tid.domain.platform.AlertNotifier
import br.com.fiap.tid.domain.platform.NotifyResult
import br.com.fiap.tid.domain.repository.TerritoryRepository
import br.com.fiap.tid.domain.usecase.AlertDetail
import br.com.fiap.tid.domain.usecase.GetAlertDetailUseCase
import br.com.fiap.tid.domain.usecase.RegisterDecisionUseCase
import br.com.fiap.tid.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlertViewModel(
    private val getAlertDetail: GetAlertDetailUseCase,
    private val registerDecision: RegisterDecisionUseCase,
    private val repository: TerritoryRepository,
    private val notifier: AlertNotifier,
    val defaultResponsible: String
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<AlertDetail>>(UiState.Loading)
    val state: StateFlow<UiState<AlertDetail>> = _state.asStateFlow()

    private val _sheetVisible = MutableStateFlow(false)
    val sheetVisible: StateFlow<Boolean> = _sheetVisible.asStateFlow()

    private val _formError = MutableStateFlow<String?>(null)
    val formError: StateFlow<String?> = _formError.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var lastKey: String? = null

    fun load(territoryId: String, indicatorId: String) {
        val key = "$territoryId|$indicatorId"
        if (lastKey == key && _state.value is UiState.Success) return
        lastKey = key
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = runCatching { getAlertDetail(territoryId, indicatorId) }.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { e ->
                    UiState.Error(
                        "Nao foi possivel carregar o alerta: " +
                            "${e.message ?: "erro desconhecido"}."
                    )
                }
            )
        }
    }

    fun openSheet() {
        _formError.value = null
        _sheetVisible.value = true
    }

    fun closeSheet() {
        _sheetVisible.value = false
    }

    /**
     * A recusa do formulario descreve o que estava errado NAQUELE envio.
     * Assim que a pessoa mexe no campo, a mensagem deixa de valer e sai da
     * tela - deixar texto vermelho sobre um campo ja corrigido e mentira.
     */
    fun clearFormError() {
        if (_formError.value != null) _formError.value = null
    }

    /** A decisao e sempre humana: o app so registra o que a pessoa confirmou. */
    fun confirmDecision(responsible: String, action: DecisionAction, note: String) {
        val detail = (_state.value as? UiState.Success)?.data ?: return
        val alert = detail.alert ?: return
        viewModelScope.launch {
            registerDecision(
                alert = alert,
                territoryName = detail.territory.name,
                responsible = responsible,
                action = action,
                note = note
            ).fold(
                onSuccess = {
                    _formError.value = null
                    _sheetVisible.value = false
                    _message.value = "Decisao registrada: ${action.label} por " +
                        "${responsible.trim()}."
                },
                onFailure = { e -> _formError.value = e.message }
            )
        }
    }

    fun notifyThisAlert() {
        val detail = (_state.value as? UiState.Success)?.data ?: return
        val alert = detail.alert
        if (alert == null) {
            _message.value = "Nao existe alerta ativo para notificar."
            return
        }
        _message.value = when (val r = notifier.notifyAlert(alert, detail.territory.name)) {
            is NotifyResult.Sent -> "Notificacao local enviada."
            is NotifyResult.PermissionDenied ->
                "Permissao negada. Plano B: o alerta segue visivel aqui e no painel; " +
                    "libere a permissao em Configuracoes do app."
            is NotifyResult.Unavailable -> "Notificacao indisponivel (${r.reason})."
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun notifierNeedsPermission(): Boolean =
        notifier.requiresRuntimePermission() && !notifier.hasPermission()

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AlertViewModel(
                    container.getAlertDetail,
                    container.registerDecision,
                    container.repository,
                    container.notifier,
                    container.preferences.responsibleName
                )
            }
        }
    }
}
