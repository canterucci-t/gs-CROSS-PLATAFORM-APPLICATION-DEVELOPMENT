package br.com.fiap.tid.domain.forecast

import br.com.fiap.tid.domain.model.AlertLevel
import br.com.fiap.tid.domain.model.DataOrigin
import br.com.fiap.tid.domain.model.Indicator
import br.com.fiap.tid.domain.model.Reading
import br.com.fiap.tid.domain.model.Territory
import br.com.fiap.tid.domain.model.TerritoryType
import br.com.fiap.tid.domain.model.Theme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes de unidade do motor de previsao.
 *
 * O dominio e Kotlin puro, por isso roda na JVM sem emulador:
 *   ./gradlew :app:testDebugUnitTest
 *
 * Cobre o caminho feliz e, principalmente, os casos em que o app NAO deve
 * alertar - a parte que protege o usuario de falso positivo.
 */
class ForecastEngineTest {

    private val now = 1_700_000_000_000L
    private val hour = 3_600_000L
    private val day = 24 * hour

    private val energia = Indicator(
        id = "i-energia",
        theme = Theme.ENERGIA,
        name = "Carga de energia",
        unit = "kWh/h",
        attentionThreshold = 900.0,
        criticalThreshold = 1150.0,
        description = "Curva de carga."
    )

    private val territorio = Territory(
        id = "t-1",
        name = "Jardim das Flores",
        type = TerritoryType.BAIRRO,
        latitude = 0.0,
        longitude = 0.0
    )

    /** Gera 14 dias de leituras horarias com pico diario = base + slope*dia. */
    private fun serie(base: Double, slopePerDay: Double, days: Int = 14): List<Reading> {
        val readings = mutableListOf<Reading>()
        for (d in 0 until days) {
            val peak = base + slopePerDay * d
            for (h in 0 until 24) {
                // o pico acontece as 19h; nas outras horas o valor e menor
                val factor = if (h == 19) 1.0 else 0.7
                readings += Reading(
                    indicatorId = energia.id,
                    territoryId = territorio.id,
                    timestamp = now - (days - 1 - d) * day - (23 - h) * hour,
                    value = peak * factor,
                    origin = DataOrigin.SINTETICA
                )
            }
        }
        return readings
    }

    @Test
    fun `tendencia de alta acima do limiar de atencao gera alerta de atencao`() {
        val result = ForecastEngine.analyze(energia, territorio, serie(700.0, 20.0), now)
        val emitted = result as ForecastEngine.Result.Emitted
        assertEquals(AlertLevel.ATENCAO, emitted.alert.level)
        assertTrue(emitted.alert.projectedValue >= energia.attentionThreshold)
        assertTrue(emitted.alert.projectedValue < energia.criticalThreshold)
    }

    @Test
    fun `projecao acima do limiar critico gera alerta critico que exige confirmacao humana`() {
        val result = ForecastEngine.analyze(energia, territorio, serie(900.0, 25.0), now)
        val emitted = result as ForecastEngine.Result.Emitted
        assertEquals(AlertLevel.CRITICO, emitted.alert.level)
        assertTrue(
            "alerta critico deve exigir confirmacao humana",
            emitted.alert.requiresHumanConfirmation
        )
    }

    @Test
    fun `serie estavel e distante do limiar nao gera alerta`() {
        val result = ForecastEngine.analyze(energia, territorio, serie(300.0, 0.0), now)
        assertTrue(result is ForecastEngine.Result.NotEmitted)
    }

    @Test
    fun `dado desatualizado suspende o alerta`() {
        val velho = serie(700.0, 20.0).map { it.copy(timestamp = it.timestamp - 10 * day) }
        val result = ForecastEngine.analyze(energia, territorio, velho, now)
        val notEmitted = result as ForecastEngine.Result.NotEmitted
        assertTrue(
            "o motivo precisa citar o dado desatualizado",
            notEmitted.reason.contains("desatualizados")
        )
    }

    @Test
    fun `historico curto suspende o alerta`() {
        val result = ForecastEngine.analyze(energia, territorio, serie(900.0, 25.0, days = 4), now)
        val notEmitted = result as ForecastEngine.Result.NotEmitted
        assertTrue(notEmitted.reason.contains("insuficiente"))
    }

    @Test
    fun `serie vazia suspende o alerta`() {
        val result = ForecastEngine.analyze(energia, territorio, emptyList(), now)
        assertTrue(result is ForecastEngine.Result.NotEmitted)
    }

    @Test
    fun `todo alerta emitido traz justificativa com regra periodo e fonte`() {
        val emitted = ForecastEngine.analyze(energia, territorio, serie(700.0, 20.0), now)
            as ForecastEngine.Result.Emitted
        val j = emitted.alert.justification
        assertTrue("a justificativa precisa listar variaveis", j.variables.size >= 5)
        assertTrue("a regra nao pode ser vazia", j.rule.isNotBlank())
        assertTrue("o periodo nao pode ser vazio", j.period.isNotBlank())
        assertTrue("a fonte precisa identificar o dado sintetico", j.source.contains("sintetico"))
        assertTrue("a atualizacao precisa estar preenchida", j.updatedAt > 0)
    }

    @Test
    fun `confianca fica entre 30 e 92 por cento`() {
        val emitted = ForecastEngine.analyze(energia, territorio, serie(700.0, 20.0), now)
            as ForecastEngine.Result.Emitted
        assertTrue(emitted.alert.confidence in 0.30..0.92)
    }

    @Test
    fun `regressao linear recupera o coeficiente de uma reta perfeita`() {
        val fit = ForecastEngine.linearFit(listOf(10.0, 20.0, 30.0, 40.0))
        assertEquals(10.0, fit.slope, 1e-9)
        assertEquals(10.0, fit.intercept, 1e-9)
        assertEquals(1.0, fit.r2, 1e-9)
    }

    @Test
    fun `variacao semanal compara as duas metades da serie`() {
        val variation = ForecastEngine.weeklyVariationPercent(
            listOf(100.0, 100.0, 100.0, 100.0, 110.0, 110.0, 110.0, 110.0)
        )
        assertEquals(10.0, variation, 1e-9)
    }

    @Test
    fun `pico diario agrega as leituras horarias pelo maior valor do dia`() {
        val readings = serie(500.0, 0.0, days = 3)
        val peaks = ForecastEngine.dailyPeaks(readings, readings.maxOf { it.timestamp })
        assertEquals(3, peaks.size)
        peaks.forEach { assertEquals(500.0, it, 1e-9) }
    }

    @Test
    fun `serie do grafico devolve historico seguido de projecao`() {
        val points = ForecastEngine.series(serie(700.0, 20.0), historyPoints = 48, forecastPoints = 6)
        assertEquals(54, points.size)
        assertEquals(48, points.count { !it.isForecast })
        assertEquals(6, points.count { it.isForecast })
        assertTrue(
            "a projecao precisa vir depois do historico no tempo",
            points.last().timestamp > points.first { it.isForecast }.timestamp - 1
        )
    }
}
