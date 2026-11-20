package br.com.fiap.tid.ui.screens.dashboard

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.fiap.tid.AppContainer
import br.com.fiap.tid.domain.model.Alert
import br.com.fiap.tid.domain.model.AlertLevel
import br.com.fiap.tid.domain.platform.AlertNotifier
import br.com.fiap.tid.domain.platform.NotifyResult
import br.com.fiap.tid.domain.repository.TerritoryRepository
import br.com.fiap.tid.domain.usecase.DashboardData
import br.com.fiap.tid.domain.usecase.GetDashboardUseCase
import br.com.fiap.tid.ui.common.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ESTADO DA TELA DO PAINEL.
 *
 * Fluxo unidirecional: a tela dispara eventos (trocar territorio, recarregar),
 * o ViewModel chama o caso de uso e expoe um [StateFlow] imutavel; o Compose
 * observa e recompoe. O estado sobrevive a rotacao porque vive no ViewModel.
 */
class DashboardViewModel(
    private val getDashboard: GetDashboardUseCase,
    private val repository: TerritoryRepository,
    private val notifier: AlertNotifier
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<DashboardData>>(UiState.Loading)
    val state: StateFlow<UiState<DashboardData>> = _state.asStateFlow()

    /** Mensagem transitoria (resultado de notificacao, por exemplo). */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /**
     * Guarda a carga em andamento: uma nova chamada cancela a anterior, o que
     * evita carregamento duplicado quando a tela volta da pilha enquanto a
     * leitura anterior ainda nao terminou.
     */
    private var loadJob: Job? = null

    /**
     * Recarrega o painel. E chamado a cada retomada da tela para refletir o
     * que o usuario mudou em Configuracoes (territorio, temas, simulacao).
     */
    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = UiState.Loading
            val started = SystemClock.elapsedRealtime()
            _state.value = runCatching {
                val selected = repository.selectedTerritoryId()
                getDashboard(selected).also {
                    Log.i(
                        PERF_TAG,
                        "dashboard_ms=${SystemClock.elapsedRealtime() - started} " +
                            "kpis=${it.kpis.size} alertas=${it.alerts.size}"
                    )
                }
            }.fold(
                onSuccess = { data ->
                    when {
                        data.kpis.isEmpty() -> UiState.Empty(
                            "Nenhum indicador monitorado tem leitura local para " +
                                "${data.territory.name}. Verifique os temas em " +
                                "Configuracoes ou recarregue os dados."
                        )
                        data.isStale -> UiState.Success(
                            data,
                            staleWarning = "Dados desatualizados: a ultima leitura tem mais de " +
                                "24 h. Os alertas preditivos ficam suspensos ate a base " +
                                "ser atualizada."
                        )
                        else -> UiState.Success(data)
                    }
                },
                onFailure = { e ->
                    val detail = (e.message ?: "erro desconhecido").trimEnd('.')
                    UiState.Error("Nao foi possivel ler os dados locais: $detail.")
                }
            )
        }
    }

    fun selectTerritory(territoryId: String) {
        viewModelScope.launch {
            repository.selectTerritory(territoryId)
            load()
        }
    }

    /**
     * Recurso nativo: dispara a notificacao local do alerta mais grave.
     * Os tres caminhos (enviado, negado, instavel) sao tratados e informados.
     */
    fun notifyMostCritical() {
        val data = (_state.value as? UiState.Success)?.data ?: return
        val alert: Alert? = data.alerts.firstOrNull { it.level == AlertLevel.CRITICO }
            ?: data.alerts.firstOrNull()
        if (alert == null) {
            _message.value = "Nenhum alerta ativo para notificar."
            return
        }
        _message.value = when (val result = notifier.notifyAlert(alert, data.territory.name)) {
            is NotifyResult.Sent -> "Notificacao enviada: ${alert.title}"
            is NotifyResult.PermissionDenied ->
                "Permissao de notificacao negada. Plano B: o alerta continua visivel " +
                    "no painel e voce pode liberar a permissao em Configuracoes."
            is NotifyResult.Unavailable ->
                "Notificacao indisponivel (${result.reason}). O alerta permanece no painel."
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    companion object {
        /** Marcador usado no plano de evidencias de qualidade. */
        private const val PERF_TAG = "TID_PERF"

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DashboardViewModel(
                    container.getDashboard,
                    container.repository,
                    container.notifier
                )
            }
        }
    }
}
