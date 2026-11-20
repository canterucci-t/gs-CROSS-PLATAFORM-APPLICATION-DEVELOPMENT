package br.com.fiap.tid.data.seed

import android.content.Context
import android.os.SystemClock
import android.util.Log
import br.com.fiap.tid.data.local.IndicatorEntity
import br.com.fiap.tid.data.local.ReadingEntity
import br.com.fiap.tid.data.local.TerritoryEntity
import br.com.fiap.tid.data.local.TidDao
import org.json.JSONArray

/**
 * CARGA INICIAL DOS DADOS.
 *
 * Le os arquivos JSON de `assets/seed/` (gerados por
 * `tools/gerar_dados_sinteticos.py`) e popula o Room na primeira execucao.
 * A operacao e idempotente: se o banco ja tem territorios, nada e feito.
 *
 * O campo `hoursAgo` do arquivo e convertido para timestamp real no momento
 * da carga, por isso a base nunca nasce desatualizada.
 */
class SeedLoader(private val context: Context, private val dao: TidDao) {

    private companion object {
        /** Marcador usado no plano de evidencias de qualidade. */
        const val PERF_TAG = "TID_PERF"
    }

    suspend fun seedIfEmpty(now: Long = System.currentTimeMillis()) {
        if (dao.territoryCount() > 0) return

        val started = SystemClock.elapsedRealtime()
        val readings = readReadings(now)
        // Uma unica transacao para toda a carga (ver TidDao.seedAll).
        dao.seedAll(readTerritories(), readIndicators(), readings)
        Log.i(
            PERF_TAG,
            "seed_ms=${SystemClock.elapsedRealtime() - started} rows=${readings.size}"
        )
    }

    private fun readTerritories(): List<TerritoryEntity> =
        asset("seed/territories.json").let { json ->
            (0 until json.length()).map { i ->
                val o = json.getJSONObject(i)
                TerritoryEntity(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    type = o.getString("type"),
                    latitude = o.getDouble("latitude"),
                    longitude = o.getDouble("longitude")
                )
            }
        }

    private fun readIndicators(): List<IndicatorEntity> =
        asset("seed/indicators.json").let { json ->
            (0 until json.length()).map { i ->
                val o = json.getJSONObject(i)
                IndicatorEntity(
                    id = o.getString("id"),
                    theme = o.getString("theme"),
                    name = o.getString("name"),
                    unit = o.getString("unit"),
                    attentionThreshold = o.getDouble("attentionThreshold"),
                    criticalThreshold = o.getDouble("criticalThreshold"),
                    description = o.getString("description")
                )
            }
        }

    /**
     * Le o arquivo COLUNAR de leituras: uma serie por (indicador, territorio),
     * com os valores em um array e o instante derivado de `startHoursAgo` e
     * `stepHours`. Guardar as chaves uma vez por serie, em vez de uma vez por
     * leitura, deixou o arquivo 17x menor e a carga muito mais rapida
     * (Prova 1 do plano de evidencias de qualidade).
     */
    private fun readReadings(now: Long): List<ReadingEntity> {
        val series = asset("seed/readings.json")
        val result = ArrayList<ReadingEntity>(8_000)
        for (s in 0 until series.length()) {
            val serie = series.getJSONObject(s)
            val indicatorId = serie.getString("indicatorId")
            val territoryId = serie.getString("territoryId")
            val origin = serie.getString("origin")
            val startHoursAgo = serie.getLong("startHoursAgo")
            val stepHours = serie.getLong("stepHours")
            val values = serie.getJSONArray("values")
            for (i in 0 until values.length()) {
                val hoursAgo = startHoursAgo - i * stepHours
                result += ReadingEntity(
                    indicatorId = indicatorId,
                    territoryId = territoryId,
                    timestamp = now - hoursAgo * 3_600_000L,
                    value = values.getDouble(i),
                    origin = origin
                )
            }
        }
        return result
    }

    private fun asset(path: String): JSONArray =
        context.assets.open(path).bufferedReader().use { JSONArray(it.readText()) }
}
