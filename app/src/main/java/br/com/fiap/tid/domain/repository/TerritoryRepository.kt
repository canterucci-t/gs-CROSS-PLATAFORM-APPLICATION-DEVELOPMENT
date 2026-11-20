package br.com.fiap.tid.domain.repository

import br.com.fiap.tid.domain.model.Decision
import br.com.fiap.tid.domain.model.DecisionAction
import br.com.fiap.tid.domain.model.Indicator
import br.com.fiap.tid.domain.model.Reading
import br.com.fiap.tid.domain.model.Territory
import kotlinx.coroutines.flow.Flow

/**
 * CONTRATO da camada de dados, declarado no dominio.
 *
 * O dominio depende desta interface, nunca do Room nem dos arquivos de assets.
 * Trocar a base sintetica por uma API publica real exige apenas uma nova
 * implementacao desta interface - nenhum caso de uso muda.
 */
interface TerritoryRepository {

    companion object {
        /** 14 dias usados pelo motor + 1 dia de margem. */
        const val ANALYSIS_WINDOW_DAYS = 15
    }

    /** Garante que o banco local esta populado (carga inicial idempotente). */
    suspend fun ensureSeeded()

    suspend fun territories(): List<Territory>

    suspend fun indicators(): List<Indicator>

    /**
     * Leituras de um territorio nos ultimos [days] dias.
     *
     * O padrao e [ANALYSIS_WINDOW_DAYS]: o motor de previsao usa 14 dias de
     * picos diarios, entao ler alem disso so gastaria memoria e tempo.
     */
    suspend fun readings(
        territoryId: String,
        days: Int = ANALYSIS_WINDOW_DAYS
    ): List<Reading>

    suspend fun readings(
        territoryId: String,
        indicatorId: String,
        days: Int = ANALYSIS_WINDOW_DAYS
    ): List<Reading>

    /** Instante da leitura mais recente do territorio, ou null se nao houver. */
    suspend fun lastReadingAt(territoryId: String): Long?

    /** Historico de decisoes humanas, observado reativamente. */
    fun observeDecisions(): Flow<List<Decision>>

    suspend fun registerDecision(
        alertId: String,
        alertTitle: String,
        territoryName: String,
        responsible: String,
        action: DecisionAction,
        note: String
    )

    /** LGPD: o usuario pode apagar o historico de decisoes do dispositivo. */
    suspend fun clearDecisions()

    // ----------------------------------------------------- estado compartilhado
    /** Territorio selecionado, persistido para sobreviver ao fim do processo. */
    suspend fun selectedTerritoryId(): String?

    suspend fun selectTerritory(territoryId: String)

    /** Temas que o usuario escolheu monitorar. */
    suspend fun monitoredThemes(): Set<String>

    suspend fun setThemeMonitored(theme: String, monitored: Boolean)

    /** Modo de simulacao de falha, usado para demonstrar a degradacao do app. */
    suspend fun dataFailureMode(): DataFailureMode

    suspend fun setDataFailureMode(mode: DataFailureMode)
}

/**
 * Cenarios de falha que o app precisa degradar com clareza.
 * Expostos em Configuracoes para que a banca veja o plano B funcionando.
 */
enum class DataFailureMode(val label: String, val description: String) {
    NENHUM("Normal", "Leituras locais disponiveis e atualizadas."),
    SEM_DADOS("Simular dados ausentes", "O repositorio devolve lista vazia."),
    DESATUALIZADO("Simular dados desatualizados", "As leituras ficam 10 dias no passado."),
    ERRO_LEITURA("Simular falha de leitura", "O repositorio lanca excecao ao ler o banco.")
}
