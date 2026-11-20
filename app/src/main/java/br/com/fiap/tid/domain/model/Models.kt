package br.com.fiap.tid.domain.model

/**
 * CAMADA DE DOMINIO - Kotlin puro.
 *
 * Nenhuma classe deste arquivo conhece Android, Compose ou Room: sao os
 * contratos de negocio do Territory Intelligence Dashboard. Trocar o banco
 * local, a fonte de dados ou a camada de interface nao altera nada aqui.
 */

/** Tipo de territorio monitorado. */
enum class TerritoryType(val label: String) {
    CIDADE("Cidade"),
    BAIRRO("Bairro"),
    AREA_RURAL("Area rural")
}

/** Tema do indicador monitorado. */
enum class Theme(val label: String) {
    ENERGIA("Energia"),
    AGUA("Agua"),
    CLIMA("Clima"),
    RESIDUOS("Residuos"),
    SERVICOS("Servicos")
}

/** Procedencia do dado - exibida na tela para nao confundir o usuario. */
enum class DataOrigin(val label: String) {
    PUBLICA("Dado publico"),
    SINTETICA("Dado sintetico"),
    AUTORIZADA("Dado autorizado")
}

/** Nivel do alerta preditivo. */
enum class AlertLevel(val label: String, val weight: Int) {
    INFORMATIVO("Informativo", 1),
    ATENCAO("Atencao", 2),
    CRITICO("Critico", 3)
}

/** Acoes que o gestor pode registrar sobre um alerta. */
enum class DecisionAction(val label: String) {
    ACATAR("Acatar"),
    ADIAR("Adiar"),
    DESCARTAR("Descartar")
}

data class Territory(
    val id: String,
    val name: String,
    val type: TerritoryType,
    val latitude: Double,
    val longitude: Double
)

data class Indicator(
    val id: String,
    val theme: Theme,
    val name: String,
    val unit: String,
    val attentionThreshold: Double,
    val criticalThreshold: Double,
    val description: String
)

data class Reading(
    val indicatorId: String,
    val territoryId: String,
    val timestamp: Long,
    val value: Double,
    val origin: DataOrigin
)

/** Indicador resumido para o painel (estado derivado, nao persistido). */
data class Kpi(
    val indicator: Indicator,
    val currentValue: Double,
    val weeklyVariationPercent: Double,
    val level: AlertLevel?,
    val lastReadingAt: Long,
    val origin: DataOrigin
)

/**
 * Justificativa obrigatoria de todo alerta: quais variaveis, qual regra,
 * qual periodo e qual fonte o originaram.
 */
data class Justification(
    val variables: List<Pair<String, String>>,
    val rule: String,
    val period: String,
    val source: String,
    val updatedAt: Long
)

data class Alert(
    val id: String,
    val indicatorId: String,
    val territoryId: String,
    val theme: Theme,
    val title: String,
    val summary: String,
    val level: AlertLevel,
    /** 0.0 a 1.0 */
    val confidence: Double,
    val justification: Justification,
    val forecastWindowHours: Int,
    val projectedValue: Double,
    val unit: String,
    val recommendation: String,
    /**
     * Quando true, a acao sugerida tem alto impacto na populacao
     * (ex.: racionamento) e SO pode ser executada apos confirmacao humana.
     */
    val requiresHumanConfirmation: Boolean
)

data class Decision(
    val id: Long,
    val alertId: String,
    val alertTitle: String,
    val territoryName: String,
    val responsible: String,
    val action: DecisionAction,
    val note: String,
    val createdAt: Long
)

/** Ponto da serie exibida no grafico: historico ou projecao. */
data class SeriesPoint(
    val timestamp: Long,
    val value: Double,
    val isForecast: Boolean
)
