package br.com.fiap.tid.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * CAMADA DE DADOS - entidades do banco local (Room sobre SQLite).
 *
 * Os enums sao gravados como String para manter o schema legivel e evitar
 * migracao quando um novo tema e adicionado.
 */

@Entity(tableName = "territories")
data class TerritoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val latitude: Double,
    val longitude: Double
)

@Entity(tableName = "indicators")
data class IndicatorEntity(
    @PrimaryKey val id: String,
    val theme: String,
    val name: String,
    val unit: String,
    val attentionThreshold: Double,
    val criticalThreshold: Double,
    val description: String
)

@Entity(
    tableName = "readings",
    indices = [Index(value = ["territoryId", "indicatorId", "timestamp"])]
)
data class ReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val indicatorId: String,
    val territoryId: String,
    val timestamp: Long,
    val value: Double,
    val origin: String
)

@Entity(tableName = "decisions")
data class DecisionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val alertId: String,
    val alertTitle: String,
    val territoryName: String,
    val responsible: String,
    val action: String,
    val note: String,
    val createdAt: Long
)
