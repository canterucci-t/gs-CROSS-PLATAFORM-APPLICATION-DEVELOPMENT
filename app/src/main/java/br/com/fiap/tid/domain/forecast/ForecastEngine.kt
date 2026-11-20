package br.com.fiap.tid.domain.forecast

import br.com.fiap.tid.domain.model.Alert
import br.com.fiap.tid.domain.model.AlertLevel
import br.com.fiap.tid.domain.model.Indicator
import br.com.fiap.tid.domain.model.Justification
import br.com.fiap.tid.domain.model.Reading
import br.com.fiap.tid.domain.model.SeriesPoint
import br.com.fiap.tid.domain.model.Territory
import br.com.fiap.tid.domain.model.Theme
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * MOTOR DE PREVISAO - Kotlin puro, deterministico e explicavel.
 *
 * METODO (escolhido por ser auditavel, nao por ser sofisticado):
 *  1. Agrega as leituras horarias em PICO DIARIO (o maior valor de cada dia),
 *     porque o risco territorial acontece no pico, nao na media.
 *  2. Calcula a MEDIA MOVEL de 7 dias e a VARIACAO SEMANAL (ultimos 7 dias
 *     contra os 7 anteriores).
 *  3. Ajusta uma REGRESSAO LINEAR por minimos quadrados sobre os picos
 *     diarios e PROJETA o valor para o horizonte de [HORIZON_DAYS] dias.
 *  4. Compara a projecao com os limiares de referencia do indicador para
 *     definir o nivel do alerta.
 *  5. Monta a JUSTIFICATIVA com as variaveis, a regra, o periodo e a fonte.
 *
 * QUANDO O APP NAO EMITE ALERTA (transparencia da incerteza):
 *  - menos de [MIN_DAYS] dias de historico;
 *  - dado mais recente com mais de [MAX_DATA_AGE_HOURS] horas (desatualizado);
 *  - tendencia nao identificavel (R2 abaixo de [MIN_R2]) e projecao abaixo do
 *    limiar critico - preferimos o silencio ao falso positivo;
 *  - projecao abaixo de 90% do limiar de atencao.
 */
object ForecastEngine {

    const val HORIZON_DAYS = 2
    const val HORIZON_HOURS = HORIZON_DAYS * 24
    const val MIN_DAYS = 8
    const val MAX_DATA_AGE_HOURS = 24
    const val MIN_R2 = 0.20
    private const val HISTORY_DAYS = 14
    private const val HOUR_MS = 3_600_000L
    private const val DAY_MS = 24 * HOUR_MS

    /** Resultado da analise de um indicador em um territorio. */
    sealed interface Result {
        data class Emitted(val alert: Alert) : Result
        /** Nao houve alerta - [reason] e exibida ao usuario, nunca escondida. */
        data class NotEmitted(val reason: String) : Result
    }

    /**
     * Analisa a serie e devolve um alerta com justificativa ou o motivo de
     * nao ter emitido.
     *
     * @param now instante de referencia (injetado para permitir teste).
     */
    fun analyze(
        indicator: Indicator,
        territory: Territory,
        readings: List<Reading>,
        now: Long,
        /**
         * Picos diarios ja calculados pelo chamador. Serve para o painel, que
         * precisa dos mesmos picos para montar o KPI: calcular uma vez so
         * evita percorrer a serie inteira duas vezes por indicador.
         */
        precomputedPeaks: List<Double>? = null
    ): Result {
        if (readings.isEmpty()) {
            return Result.NotEmitted("Sem leituras para este indicador.")
        }

        val lastReadingAt = readings.maxOf { it.timestamp }
        val ageHours = ((now - lastReadingAt) / HOUR_MS).toInt()
        if (ageHours > MAX_DATA_AGE_HOURS) {
            return Result.NotEmitted(
                "Dados desatualizados (ultima leitura ha ${ageHours}h). " +
                    "O app nao emite alerta sobre dado vencido."
            )
        }

        val peaks = (precomputedPeaks ?: dailyPeaks(readings, lastReadingAt))
            .takeLast(HISTORY_DAYS)
        if (peaks.size < MIN_DAYS) {
            return Result.NotEmitted(
                "Historico insuficiente: ${peaks.size} dia(s) de ${MIN_DAYS} necessarios."
            )
        }

        val fit = linearFit(peaks)
        val projected = fit.valueAt((peaks.size - 1 + HORIZON_DAYS).toDouble())
        val movingAverage = peaks.takeLast(7).average()
        val weeklyVariation = weeklyVariationPercent(peaks)

        val level = classify(indicator, projected)
        if (level == null) {
            return Result.NotEmitted(
                "Projecao de ${fmt(projected)} ${indicator.unit} para " +
                    "${HORIZON_DAYS} dias fica abaixo do limiar de atencao " +
                    "(${fmt(indicator.attentionThreshold)} ${indicator.unit})."
            )
        }
        if (fit.r2 < MIN_R2 && projected < indicator.criticalThreshold) {
            return Result.NotEmitted(
                "Tendencia nao identificavel (R2 = ${fmt2(fit.r2)}). " +
                    "O app prefere nao alertar a gerar falso positivo."
            )
        }

        val origin = readings.last().origin
        val confidence = confidenceOf(fit.r2, peaks.size)
        val justification = Justification(
            variables = listOf(
                "Pico mais recente" to "${fmt(peaks.last())} ${indicator.unit}",
                "Media movel (7 dias)" to "${fmt(movingAverage)} ${indicator.unit}",
                "Variacao semanal" to "${signed(weeklyVariation)}%",
                "Tendencia (regressao linear)" to
                    "${signed(fit.slope)} ${indicator.unit}/dia",
                "Projecao em ${HORIZON_DAYS} dias" to "${fmt(projected)} ${indicator.unit}",
                "Limiar de atencao" to "${fmt(indicator.attentionThreshold)} ${indicator.unit}",
                "Limiar critico" to "${fmt(indicator.criticalThreshold)} ${indicator.unit}",
                "Aderencia do ajuste (R2)" to fmt2(fit.r2)
            ),
            rule = "Pico diario agregado, media movel de 7 dias e regressao linear " +
                "sobre ${peaks.size} dias; a projecao para ${HORIZON_HOURS}h " +
                "(${fmt(projected)} ${indicator.unit}) alcanca " +
                "${percentOfThreshold(projected, indicator)}% do limiar de " +
                "${if (level == AlertLevel.CRITICO) "critico" else "atencao"}.",
            period = "Ultimos ${peaks.size} dias de leituras horarias; " +
                "projecao para as proximas ${HORIZON_HOURS} horas.",
            source = "${origin.label} - base documentada em tools/gerar_dados_sinteticos.py",
            updatedAt = lastReadingAt
        )

        val alert = Alert(
            id = alertId(indicator.id, territory.id),
            indicatorId = indicator.id,
            territoryId = territory.id,
            theme = indicator.theme,
            title = titleOf(level, indicator, territory),
            summary = "Projecao de ${fmt(projected)} ${indicator.unit} nas proximas " +
                "${HORIZON_HOURS}h (${signed(weeklyVariation)}% na semana).",
            level = level,
            confidence = confidence,
            justification = justification,
            forecastWindowHours = HORIZON_HOURS,
            projectedValue = projected,
            unit = indicator.unit,
            recommendation = recommendationOf(level, indicator),
            requiresHumanConfirmation = level == AlertLevel.CRITICO
        )
        return Result.Emitted(alert)
    }

    /** Serie para o grafico: historico recente + faixa de projecao. */
    fun series(
        readings: List<Reading>,
        historyPoints: Int = 72,
        forecastPoints: Int = 12
    ): List<SeriesPoint> {
        if (readings.isEmpty()) return emptyList()
        val sorted = readings.sortedBy { it.timestamp }
        val history = sorted.takeLast(historyPoints)
            .map { SeriesPoint(it.timestamp, it.value, isForecast = false) }
        if (history.size < 4) return history

        val fit = linearFit(history.map { it.value })
        val stepMs = ((history.last().timestamp - history.first().timestamp) /
            (history.size - 1)).coerceAtLeast(HOUR_MS)
        val forecast = (1..forecastPoints).map { i ->
            SeriesPoint(
                timestamp = history.last().timestamp + stepMs * i,
                value = fit.valueAt((history.size - 1 + i).toDouble()),
                isForecast = true
            )
        }
        return history + forecast
    }

    /** Variacao percentual entre a ultima semana e a anterior. */
    fun weeklyVariationPercent(values: List<Double>): Double {
        if (values.size < 4) return 0.0
        val half = values.size / 2
        val previous = values.take(half).average()
        val current = values.drop(half).average()
        if (abs(previous) < 1e-6) return 0.0
        return (current / previous - 1.0) * 100.0
    }

    fun alertId(indicatorId: String, territoryId: String): String = "$indicatorId@$territoryId"

    /** Agrega leituras horarias em um pico por dia, em ordem cronologica. */
    internal fun dailyPeaks(readings: List<Reading>, reference: Long): List<Double> =
        readings
            .groupBy { ((reference - it.timestamp) / DAY_MS).toInt() }
            .toSortedMap(reverseOrder())
            .map { (_, group) -> group.maxOf { it.value } }

    internal fun classify(indicator: Indicator, projected: Double): AlertLevel? = when {
        projected >= indicator.criticalThreshold -> AlertLevel.CRITICO
        projected >= indicator.attentionThreshold -> AlertLevel.ATENCAO
        projected >= indicator.attentionThreshold * 0.9 -> AlertLevel.INFORMATIVO
        else -> null
    }

    /** Confianca derivada da aderencia do ajuste e do tamanho do historico. */
    internal fun confidenceOf(r2: Double, days: Int): Double {
        val base = 0.40 + 0.50 * r2.coerceIn(0.0, 1.0)
        val penalty = if (days < 10) 0.10 else 0.0
        return (base - penalty).coerceIn(0.30, 0.92)
    }

    // ------------------------------------------------------------- regressao
    internal data class Fit(val slope: Double, val intercept: Double, val r2: Double) {
        fun valueAt(x: Double): Double = intercept + slope * x
    }

    internal fun linearFit(values: List<Double>): Fit {
        val n = values.size
        if (n < 2) return Fit(0.0, values.firstOrNull() ?: 0.0, 0.0)
        val meanX = (n - 1) / 2.0
        val meanY = values.average()
        var sxx = 0.0
        var sxy = 0.0
        values.forEachIndexed { i, y ->
            val dx = i - meanX
            sxx += dx * dx
            sxy += dx * (y - meanY)
        }
        val slope = if (sxx == 0.0) 0.0 else sxy / sxx
        val intercept = meanY - slope * meanX
        var ssTot = 0.0
        var ssRes = 0.0
        values.forEachIndexed { i, y ->
            ssTot += (y - meanY) * (y - meanY)
            val predicted = intercept + slope * i
            ssRes += (y - predicted) * (y - predicted)
        }
        val r2 = if (ssTot <= 1e-9) 0.0 else (1.0 - ssRes / ssTot).coerceIn(0.0, 1.0)
        return Fit(slope, intercept, r2)
    }

    // --------------------------------------------------------------- textos
    private fun titleOf(level: AlertLevel, indicator: Indicator, territory: Territory): String {
        val risk = when (indicator.theme) {
            Theme.ENERGIA -> "risco de sobrecarga de energia"
            Theme.AGUA -> "risco de queda de pressao de agua"
            Theme.CLIMA -> "risco de calor extremo"
            Theme.RESIDUOS -> "risco de acumulo de residuos"
            Theme.SERVICOS -> "risco de fila no atendimento"
        }
        return "${level.label}: $risk em ${territory.name} nas proximas ${HORIZON_HOURS}h"
    }

    private fun recommendationOf(level: AlertLevel, indicator: Indicator): String = when (level) {
        AlertLevel.INFORMATIVO ->
            "Acompanhar o indicador ${indicator.name} nas proximas leituras."
        AlertLevel.ATENCAO ->
            "Reforcar o monitoramento e avisar a equipe responsavel pelo territorio."
        AlertLevel.CRITICO ->
            "Avaliar acao de contencao com a equipe. ATENCAO: medidas de alto impacto " +
                "(racionamento, interrupcao de servico, evacuacao) precisam de " +
                "confirmacao de um responsavel - o app nunca as executa sozinho."
    }

    private fun percentOfThreshold(projected: Double, indicator: Indicator): Int {
        val threshold = if (projected >= indicator.criticalThreshold) {
            indicator.criticalThreshold
        } else {
            indicator.attentionThreshold
        }
        if (threshold <= 0.0) return 0
        return (projected / threshold * 100.0).roundToInt()
    }

    private fun fmt(v: Double): String = ((v * 10).roundToInt() / 10.0).toString()
    private fun fmt2(v: Double): String = ((v * 100).roundToInt() / 100.0).toString()
    private fun signed(v: Double): String {
        val r = (v * 10).roundToInt() / 10.0
        return if (r >= 0) "+$r" else "$r"
    }
}
