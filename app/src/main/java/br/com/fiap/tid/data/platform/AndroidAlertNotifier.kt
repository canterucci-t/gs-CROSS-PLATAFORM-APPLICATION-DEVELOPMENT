package br.com.fiap.tid.data.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.com.fiap.tid.R
import br.com.fiap.tid.domain.model.Alert
import br.com.fiap.tid.domain.platform.AlertNotifier
import br.com.fiap.tid.domain.platform.NotifyResult

/**
 * Implementacao Android do adaptador de notificacao local.
 *
 * Unico ponto do app que conhece NotificationManager. Trata os tres estados do
 * recurso: concedido, negado (plano B na tela) e instavel (canal desligado,
 * excecao do sistema).
 */
class AndroidAlertNotifier(private val context: Context) : AlertNotifier {

    override fun hasPermission(): Boolean {
        if (!requiresRuntimePermission()) {
            // Abaixo do Android 13 basta o canal/notificacoes estarem ativos.
            return NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun requiresRuntimePermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    override fun notifyAlert(alert: Alert, territoryName: String): NotifyResult {
        if (!hasPermission()) return NotifyResult.PermissionDenied
        return try {
            ensureChannel()
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_alert)
                .setContentTitle(alert.title)
                .setContentText(alert.summary)
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        buildString {
                            appendLine(alert.summary)
                            appendLine()
                            appendLine("Justificativa: ${alert.justification.rule}")
                            append("Confianca: ${(alert.confidence * 100).toInt()}% - ")
                            append(alert.justification.source)
                        }
                    )
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context)
                .notify(alert.id.hashCode(), notification)
            NotifyResult.Sent
        } catch (e: SecurityException) {
            NotifyResult.PermissionDenied
        } catch (e: Exception) {
            NotifyResult.Unavailable(e.message ?: "Falha ao acionar a notificacao.")
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Alertas territoriais",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas preditivos criticos do territorio monitorado."
            }
        )
    }

    companion object {
        const val CHANNEL_ID = "tid_alertas_criticos"
    }
}
