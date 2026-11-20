package br.com.fiap.tid.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Banco local do app. Offline e o regime normal: o painel abre sem internet,
 * lendo exclusivamente deste banco.
 */
@Database(
    entities = [
        TerritoryEntity::class,
        IndicatorEntity::class,
        ReadingEntity::class,
        DecisionEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class TidDatabase : RoomDatabase() {

    abstract fun dao(): TidDao

    companion object {
        private const val NAME = "tid.db"

        @Volatile
        private var instance: TidDatabase? = null

        fun get(context: Context): TidDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                TidDatabase::class.java,
                NAME
            )
                // Base sintetica e recarregavel: em caso de mudanca de schema
                // destruimos e repopulamos, sem risco para o usuario.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                .also { instance = it }
        }
    }
}
