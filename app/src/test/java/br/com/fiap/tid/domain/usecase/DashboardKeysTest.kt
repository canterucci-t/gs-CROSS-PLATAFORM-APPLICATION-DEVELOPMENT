package br.com.fiap.tid.domain.usecase

import br.com.fiap.tid.domain.model.DataOrigin
import br.com.fiap.tid.domain.model.Decision
import br.com.fiap.tid.domain.model.DecisionAction
import br.com.fiap.tid.domain.model.Indicator
import br.com.fiap.tid.domain.model.Reading
import br.com.fiap.tid.domain.model.Territory
import br.com.fiap.tid.domain.model.TerritoryType
import br.com.fiap.tid.domain.model.Theme
import br.com.fiap.tid.domain.repository.DataFailureMode
import br.com.fiap.tid.domain.repository.TerritoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TESTE DE REGRESSAO - crash de chave duplicada no painel (19/11/2026).
 *
 * O painel e uma unica LazyColumn que lista, em secoes diferentes, os KPIs de
 * TODOS os indicadores e tambem os indicadores que NAO geraram alerta. Um
 * indicador silenciado aparece nas duas secoes, porque o app mostra o KPI dele
 * de qualquer jeito e explica o silencio logo abaixo.
 *
 * Quando as duas secoes usavam o id do indicador como chave, o Compose
 * derrubava o app ao compor as duas ao mesmo tempo:
 *
 *   java.lang.IllegalArgumentException: Key "i-agua" was already used.
 *
 * Este teste prova no dominio que a sobreposicao existe e e esperada, e fixa
 * o contrato de que as chaves da interface precisam ser prefixadas por secao.
 */
class DashboardKeysTest {

    private val hour = 3_600_000L
    private val agora = 1_700_000_000_000L

    /** Indicador tranquilo: serie plana muito abaixo do limiar de atencao. */
    private val agua = Indicator(
        id = "i-agua",
        theme = Theme.AGUA,
        name = "Consumo de agua",
        unit = "L/s",
        attentionThreshold = 100.0,
        criticalThreshold = 130.0,
        description = "Serie plana, usada para forcar o silencio."
    )

    private val territorio = Territory(
        id = "t-1",
        name = "Jardim das Flores",
        type = TerritoryType.BAIRRO,
        latitude = 0.0,
        longitude = 0.0
    )

    /** 15 dias de leituras horarias planas: historico suficiente, risco nenhum. */
    private val leituras: List<Reading> = (0 until 15 * 24).map { i ->
        Reading(
            indicatorId = agua.id,
            territoryId = territorio.id,
            timestamp = agora - (15 * 24 - 1 - i) * hour,
            value = 10.0 + (i % 3) * 0.1,
            origin = DataOrigin.SINTETICA
        )
    }

    private val repositorio = object : TerritoryRepository {
        override suspend fun ensureSeeded() = Unit
        override suspend fun territories() = listOf(territorio)
        override suspend fun indicators() = listOf(agua)
        override suspend fun readings(territoryId: String, days: Int) = leituras
        override suspend fun readings(
            territoryId: String,
            indicatorId: String,
            days: Int
        ) = leituras

        override suspend fun lastReadingAt(territoryId: String) = agora
        override fun observeDecisions(): Flow<List<Decision>> = flowOf(emptyList())
        override suspend fun registerDecision(
            alertId: String,
            alertTitle: String,
            territoryName: String,
            responsible: String,
            action: DecisionAction,
            note: String
        ) = Unit

        override suspend fun clearDecisions() = Unit
        override suspend fun selectedTerritoryId() = territorio.id
        override suspend fun selectTerritory(territoryId: String) = Unit
        override suspend fun monitoredThemes() = Theme.entries.map { it.name }.toSet()
        override suspend fun setThemeMonitored(theme: String, monitored: Boolean) = Unit
        override suspend fun dataFailureMode() = DataFailureMode.NENHUM
        override suspend fun setDataFailureMode(mode: DataFailureMode) = Unit
    }

    private fun painel() = runBlocking {
        GetDashboardUseCase(repositorio)(territorio.id, agora)
    }

    @Test
    fun `indicador silenciado aparece no KPI e tambem na lista de silenciados`() {
        val data = painel()

        assertTrue("o indicador plano nao deveria gerar alerta", data.alerts.isEmpty())
        assertEquals(listOf(agua.id), data.kpis.map { it.indicator.id })
        assertEquals(listOf(agua.id), data.silencedReasons.map { it.first.id })
    }

    @Test
    fun `chave crua do indicador colide entre as duas secoes do painel`() {
        val data = painel()

        val cruas = data.kpis.map { it.indicator.id } +
            data.silencedReasons.map { it.first.id }

        assertTrue(
            "sem prefixo de secao as chaves colidem - foi o que derrubava o app",
            cruas.size != cruas.distinct().size
        )
    }

    @Test
    fun `chave prefixada por secao e unica na LazyColumn inteira`() {
        val data = painel()

        // Exatamente as chaves usadas em DashboardScreen.kt.
        val chaves = data.alerts.map { "alerta-${it.id}" } +
            data.kpis.map { "kpi-${it.indicator.id}" } +
            data.silencedReasons.map { "silenciado-${it.first.id}" }

        assertEquals(
            "toda chave da LazyColumn do painel precisa ser unica",
            chaves.size,
            chaves.distinct().size
        )
    }

    @Test
    fun `silencio vem com motivo escrito para o usuario`() {
        val data = painel()

        val (_, motivo) = data.silencedReasons.single()
        assertTrue("o motivo nao pode vir vazio", motivo.isNotBlank())
    }
}
