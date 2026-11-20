package br.com.fiap.tid.domain.platform

import br.com.fiap.tid.domain.model.Alert

/**
 * ADAPTADOR DE RECURSO NATIVO (contrato no dominio).
 *
 * A tela NUNCA chama a API de notificacao do Android diretamente: ela conhece
 * apenas esta interface. Isso mantem o dominio testavel e permite trocar o
 * mecanismo (notificacao, SMS, webhook) sem mexer na interface.
 *
 * Todo recurso externo tem tres estados - disponivel, negado e instavel -
 * e os tres sao modelados em [NotifyResult].
 */
interface AlertNotifier {

    /** true quando a permissao de notificacao ja foi concedida. */
    fun hasPermission(): Boolean

    /** true quando o sistema exige pedido explicito (Android 13+). */
    fun requiresRuntimePermission(): Boolean

    /** Envia a notificacao local do alerta critico. */
    fun notifyAlert(alert: Alert, territoryName: String): NotifyResult
}

sealed interface NotifyResult {
    /** Caminho feliz: notificacao entregue ao sistema. */
    data object Sent : NotifyResult

    /** Permissao negada - a tela deve exibir o plano B. */
    data object PermissionDenied : NotifyResult

    /** Recurso instavel (canal desligado, erro do sistema). */
    data class Unavailable(val reason: String) : NotifyResult
}
