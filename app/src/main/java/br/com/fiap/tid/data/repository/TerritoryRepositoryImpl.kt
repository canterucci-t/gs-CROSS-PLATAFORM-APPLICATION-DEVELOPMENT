package br.com.fiap.tid.data.repository

import br.com.fiap.tid.data.local.DecisionEntity
import br.com.fiap.tid.data.local.IndicatorEntity
import br.com.fiap.tid.data.local.ReadingEntity
import br.com.fiap.tid.data.local.TerritoryEntity
import br.com.fiap.tid.data.local.TidDao
import br.com.fiap.tid.data.prefs.PreferencesStore
import br.com.fiap.tid.data.seed.SeedLoader
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * IMPLEMENTACAO do contrato de dados.
 *
 * Isola do dominio: o banco local (Room), a carga dos assets e as
 * preferencias. Tambem aplica o [DataFailureMode], que permite demonstrar na
 * banca os tres cenarios de degradacao exigidos.
 */
class TerritoryRepositoryImpl(
    private val dao: TidDao,
    private val seedLoader: SeedLoader,
    private val prefs: PreferencesStore
) : TerritoryRepository {

    override suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        seedLoader.seedIfEmpty()
    }

    override suspend fun territories(): List<Territory> = withContext(Dispatchers.IO) {
        dao.territories().map { it.toDomain() }
    }

    override suspend fun indicators(): List<Indicator> = withContext(Dispatchers.IO) {
        dao.indicators().map { it.toDomain() }
    }

    override suspend fun readings(territoryId: String, days: Int): List<Reading> =
        withContext(Dispatchers.IO) {
            guard { dao.readings(territoryId, since(days)).map { it.toDomain() } }
        }

    override suspend fun readings(
        territoryId: String,
        indicatorId: String,
        days: Int
    ): List<Reading> = withContext(Dispatchers.IO) {
        guard { dao.readings(territoryId, indicatorId, since(days)).map { it.toDomain() } }
    }

    override suspend fun lastReadingAt(territoryId: String): Long? =
        withContext(Dispatchers.IO) { dao.lastReadingAt(territoryId) }

    override fun observeDecisions(): Flow<List<Decision>> =
        dao.observeDecisions().map { list -> list.map { it.toDomain() } }

    override suspend fun registerDecision(
        alertId: String,
        alertTitle: String,
        territoryName: String,
        responsible: String,
        action: DecisionAction,
        note: String
    ) = withContext(Dispatchers.IO) {
        prefs.responsibleName = responsible
        dao.insertDecision(
            DecisionEntity(
                alertId = alertId,
                alertTitle = alertTitle,
                territoryName = territoryName,
                responsible = responsible,
                action = action.name,
                note = note,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun clearDecisions() = withContext(Dispatchers.IO) { dao.clearDecisions() }

    override suspend fun selectedTerritoryId(): String? = prefs.selectedTerritoryId

    override suspend fun selectTerritory(territoryId: String) {
        prefs.selectedTerritoryId = territoryId
    }

    override suspend fun monitoredThemes(): Set<String> = prefs.monitoredThemes

    override suspend fun setThemeMonitored(theme: String, monitored: Boolean) =
        prefs.setThemeMonitored(theme, monitored)

    override suspend fun dataFailureMode(): DataFailureMode = prefs.failureMode

    override suspend fun setDataFailureMode(mode: DataFailureMode) {
        prefs.failureMode = mode
    }

    // ------------------------------------------------------- degradacao / plano B
    /**
     * Aplica o cenario de falha escolhido em Configuracoes. Em producao esta
     * funcao seria a propria politica de validade dos dados.
     */
    private inline fun guard(block: () -> List<Reading>): List<Reading> =
        when (prefs.failureMode) {
            DataFailureMode.NENHUM -> block()
            DataFailureMode.SEM_DADOS -> emptyList()
            DataFailureMode.DESATUALIZADO ->
                block().map { it.copy(timestamp = it.timestamp - TEN_DAYS_MS) }
            DataFailureMode.ERRO_LEITURA ->
                throw IllegalStateException("Falha simulada ao ler o banco local.")
        }

    private fun since(days: Int): Long = System.currentTimeMillis() - days * 86_400_000L

    // ------------------------------------------------------------- mapeamentos
    private fun TerritoryEntity.toDomain() = Territory(
        id = id,
        name = name,
        type = runCatching { TerritoryType.valueOf(type) }.getOrDefault(TerritoryType.BAIRRO),
        latitude = latitude,
        longitude = longitude
    )

    private fun IndicatorEntity.toDomain() = Indicator(
        id = id,
        theme = runCatching { Theme.valueOf(theme) }.getOrDefault(Theme.ENERGIA),
        name = name,
        unit = unit,
        attentionThreshold = attentionThreshold,
        criticalThreshold = criticalThreshold,
        description = description
    )

    private fun ReadingEntity.toDomain() = Reading(
        indicatorId = indicatorId,
        territoryId = territoryId,
        timestamp = timestamp,
        value = value,
        origin = runCatching { DataOrigin.valueOf(origin) }.getOrDefault(DataOrigin.SINTETICA)
    )

    private fun DecisionEntity.toDomain() = Decision(
        id = id,
        alertId = alertId,
        alertTitle = alertTitle,
        territoryName = territoryName,
        responsible = responsible,
        action = runCatching { DecisionAction.valueOf(action) }
            .getOrDefault(DecisionAction.ADIAR),
        note = note,
        createdAt = createdAt
    )

    private companion object {
        const val TEN_DAYS_MS = 10L * 86_400_000L
    }
}
