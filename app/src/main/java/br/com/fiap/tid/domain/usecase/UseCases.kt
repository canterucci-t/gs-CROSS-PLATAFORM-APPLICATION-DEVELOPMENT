package br.com.fiap.tid.domain.usecase

import br.com.fiap.tid.domain.forecast.ForecastEngine
import br.com.fiap.tid.domain.model.Alert
import br.com.fiap.tid.domain.model.DataOrigin
import br.com.fiap.tid.domain.model.DecisionAction
import br.com.fiap.tid.domain.model.Indicator
import br.com.fiap.tid.domain.model.Kpi
import br.com.fiap.tid.domain.model.Reading
import br.com.fiap.tid.domain.model.SeriesPoint
import br.com.fiap.tid.domain.model.Territory
import br.com.fiap.tid.domain.repository.TerritoryRepository

/**
 * CASOS DE USO - a regra de negocio mora aqui.
 * A interface nao calcula indicador nem decide alerta; o Room nao impoe fluxo.
 */

/** Painel consolidado de um territorio. */
data class DashboardData(
    val territories: List<Territory>,
    val territory: Territory,
    val kpis: List<Kpi>,
    val alerts: List<Alert>,
    /** Indicadores sem alerta e o motivo - a incerteza tambem e informada. */
    val silencedReasons: List<Pair<Indicator, String>>,
    val lastUpdatedAt: Long,
    val origin: DataOrigin,
    val isStale: Boolean
)

/**
 * Monta o painel: le as leituras locais, deriva os KPIs e roda o motor de
 * previsao para cada indicador monitorado.
 */
class GetDashboardUseCase(private val repository: TerritoryRepository) {

    suspend operator fun invoke(
        territoryId: String?,
        now: Long = System.currentTimeMillis()
    ): DashboardData {
        repository.ensureSeeded()
        val territories = repository.territories()
        require(territories.isNotEmpty()) { "Nenhum territorio cadastrado." }

        val territory = territories.firstOrNull { it.id == territoryId } ?: territories.first()
        val monitored = repository.monitoredThemes()
        val indicators = repository.indicators().filter { it.theme.name in monitored }
        val readings = repository.readings(territory.id)
        val byIndicator = readings.groupBy { it.indicatorId }

        val kpis = mutableListOf<Kpi>()
        val alerts = mutableListOf<Alert>()
        val silenced = mutableListOf<Pair<Indicator, String>>()

        indicators.forEach { indicator ->
            val series = byIndicator[indicator.id].orEmpty().sortedBy { it.timestamp }
            if (series.isEmpty()) {
                silenced += indicator to "Sem leituras locais para este indicador."
                return@forEach
            }
            // Os picos diarios sao calculados UMA vez e reaproveitados pelo
            // motor de previsao e pelo KPI.
            val peaks = ForecastEngine.dailyPeaks(series, series.last().timestamp)
            val result = ForecastEngine.analyze(indicator, territory, series, now, peaks)
            val alert = when (result) {
                is ForecastEngine.Result.Emitted -> result.alert.also { alerts += it }
                is ForecastEngine.Result.NotEmitted -> {
                    silenced += indicator to result.reason
                    null
                }
            }
            kpis += toKpi(indicator, series, alert, peaks)
        }

        val lastUpdatedAt = readings.maxOfOrNull { it.timestamp } ?: 0L
        val isStale = lastUpdatedAt == 0L ||
            (now - lastUpdatedAt) > ForecastEngine.MAX_DATA_AGE_HOURS * 3_600_000L

        return DashboardData(
            territories = territories,
            territory = territory,
            kpis = kpis,
            alerts = alerts.sortedByDescending { it.level.weight },
            silencedReasons = silenced,
            lastUpdatedAt = lastUpdatedAt,
            origin = readings.lastOrNull()?.origin ?: DataOrigin.SINTETICA,
            isStale = isStale
        )
    }

    private fun toKpi(
        indicator: Indicator,
        series: List<Reading>,
        alert: Alert?,
        peaks: List<Double>
    ): Kpi {
        return Kpi(
            indicator = indicator,
            currentValue = series.last().value,
            weeklyVariationPercent = ForecastEngine.weeklyVariationPercent(peaks),
            level = alert?.level,
            lastReadingAt = series.last().timestamp,
            origin = series.last().origin
        )
    }
}

/** Detalhe de um indicador: serie historica, projecao e alerta, se houver. */
data class IndicatorDetail(
    val territory: Territory,
    val indicator: Indicator,
    val series: List<SeriesPoint>,
    val kpi: Kpi,
    val alert: Alert?,
    val silencedReason: String?,
    val readingCount: Int
)

class GetIndicatorDetailUseCase(private val repository: TerritoryRepository) {

    suspend operator fun invoke(
        territoryId: String,
        indicatorId: String,
        now: Long = System.currentTimeMillis()
    ): IndicatorDetail {
        val territory = repository.territories().first { it.id == territoryId }
        val indicator = repository.indicators().first { it.id == indicatorId }
        val readings = repository.readings(territoryId, indicatorId).sortedBy { it.timestamp }
        require(readings.isNotEmpty()) { "Sem leituras locais para ${indicator.name}." }

        val result = ForecastEngine.analyze(indicator, territory, readings, now)
        val peaks = ForecastEngine.dailyPeaks(readings, readings.last().timestamp)
        return IndicatorDetail(
            territory = territory,
            indicator = indicator,
            series = ForecastEngine.series(readings),
            kpi = Kpi(
                indicator = indicator,
                currentValue = readings.last().value,
                weeklyVariationPercent = ForecastEngine.weeklyVariationPercent(peaks),
                level = (result as? ForecastEngine.Result.Emitted)?.alert?.level,
                lastReadingAt = readings.last().timestamp,
                origin = readings.last().origin
            ),
            alert = (result as? ForecastEngine.Result.Emitted)?.alert,
            silencedReason = (result as? ForecastEngine.Result.NotEmitted)?.reason,
            readingCount = readings.size
        )
    }
}

/** Detalhe de um alerta, recalculado a partir dos identificadores da rota. */
data class AlertDetail(
    val territory: Territory,
    val indicator: Indicator,
    val alert: Alert?,
    val silencedReason: String?,
    val series: List<SeriesPoint>
)

class GetAlertDetailUseCase(private val repository: TerritoryRepository) {

    suspend operator fun invoke(
        territoryId: String,
        indicatorId: String,
        now: Long = System.currentTimeMillis()
    ): AlertDetail {
        val territory = repository.territories().first { it.id == territoryId }
        val indicator = repository.indicators().first { it.id == indicatorId }
        val readings = repository.readings(territoryId, indicatorId).sortedBy { it.timestamp }
        val result = ForecastEngine.analyze(indicator, territory, readings, now)
        return AlertDetail(
            territory = territory,
            indicator = indicator,
            alert = (result as? ForecastEngine.Result.Emitted)?.alert,
            silencedReason = (result as? ForecastEngine.Result.NotEmitted)?.reason,
            series = ForecastEngine.series(readings)
        )
    }
}

/**
 * Registra a decisao humana. Valida o responsavel: alerta critico sem
 * responsavel identificado nao pode ser acatado - rastreabilidade e requisito.
 */
class RegisterDecisionUseCase(private val repository: TerritoryRepository) {

    suspend operator fun invoke(
        alert: Alert,
        territoryName: String,
        responsible: String,
        action: DecisionAction,
        note: String
    ): kotlin.Result<Unit> {
        val cleanResponsible = responsible.trim()
        if (cleanResponsible.length < 3) {
            return kotlin.Result.failure(
                IllegalArgumentException("Informe o responsavel pela decisao (minimo 3 letras).")
            )
        }
        if (alert.requiresHumanConfirmation && note.trim().length < 5) {
            return kotlin.Result.failure(
                IllegalArgumentException(
                    "Alerta critico exige observacao do responsavel (minimo 5 letras)."
                )
            )
        }
        return runCatching {
            repository.registerDecision(
                alertId = alert.id,
                alertTitle = alert.title,
                territoryName = territoryName,
                responsible = cleanResponsible,
                action = action,
                note = note.trim()
            )
        }
    }
}
