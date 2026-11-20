package br.com.fiap.tid.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * DAO do banco local.
 *
 * Declarado como classe abstrata (e nao interface) para permitir o metodo
 * [seedAll] anotado com @Transaction: a carga inicial inteira entra em UMA
 * transacao, o que reduz o tempo de gravacao e garante que o banco nunca
 * fique meio populado se o processo morrer no meio.
 */
@Dao
abstract class TidDao {

    // ------------------------------------------------------------- territorios
    @Query("SELECT * FROM territories ORDER BY name")
    abstract suspend fun territories(): List<TerritoryEntity>

    @Query("SELECT COUNT(*) FROM territories")
    abstract suspend fun territoryCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTerritories(items: List<TerritoryEntity>)

    // -------------------------------------------------------------- indicadores
    @Query("SELECT * FROM indicators")
    abstract suspend fun indicators(): List<IndicatorEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertIndicators(items: List<IndicatorEntity>)

    // ----------------------------------------------------------------- leituras
    @Query(
        "SELECT * FROM readings WHERE territoryId = :territoryId " +
            "AND timestamp >= :since ORDER BY timestamp"
    )
    abstract suspend fun readings(territoryId: String, since: Long): List<ReadingEntity>

    @Query(
        "SELECT * FROM readings WHERE territoryId = :territoryId " +
            "AND indicatorId = :indicatorId AND timestamp >= :since ORDER BY timestamp"
    )
    abstract suspend fun readings(
        territoryId: String,
        indicatorId: String,
        since: Long
    ): List<ReadingEntity>

    @Query("SELECT MAX(timestamp) FROM readings WHERE territoryId = :territoryId")
    abstract suspend fun lastReadingAt(territoryId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertReadings(items: List<ReadingEntity>)

    // ---------------------------------------------------------------- decisoes
    @Query("SELECT * FROM decisions ORDER BY createdAt DESC")
    abstract fun observeDecisions(): Flow<List<DecisionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertDecision(item: DecisionEntity)

    @Query("DELETE FROM decisions")
    abstract suspend fun clearDecisions()

    // ------------------------------------------------------------ carga inicial
    /**
     * Carga inicial completa em uma unica transacao.
     * As leituras vao em lotes por causa do limite de variaveis do SQLite,
     * mas todos os lotes compartilham a mesma transacao.
     */
    @Transaction
    open suspend fun seedAll(
        territories: List<TerritoryEntity>,
        indicators: List<IndicatorEntity>,
        readings: List<ReadingEntity>
    ) {
        insertTerritories(territories)
        insertIndicators(indicators)
        readings.chunked(CHUNK).forEach { insertReadings(it) }
    }

    private companion object {
        const val CHUNK = 1_000
    }
}
