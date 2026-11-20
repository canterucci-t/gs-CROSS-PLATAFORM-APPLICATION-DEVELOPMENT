package br.com.fiap.tid.ui.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Estados de tela padronizados. Toda tela do app passa por aqui: nao existe
 * caminho "so o cenario ideal".
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Empty(val message: String) : UiState<Nothing>
    data class Error(val message: String) : UiState<Nothing>
    data class Success<T>(val data: T, val staleWarning: String? = null) : UiState<T>
}

private val dateTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR"))
private val hourOnly = SimpleDateFormat("dd/MM HH'h'", Locale.forLanguageTag("pt-BR"))

fun Long.asDateTime(): String = dateTime.format(Date(this))

fun Long.asShortDateTime(): String = hourOnly.format(Date(this))

/** "ha 12 min", "ha 3 h", "ha 2 dias" - sempre explicito na tela. */
fun Long.asRelativeTime(now: Long = System.currentTimeMillis()): String {
    val minutes = ((now - this) / 60_000L).coerceAtLeast(0)
    return when {
        minutes < 1 -> "agora"
        minutes < 60 -> "ha $minutes min"
        minutes < 1_440 -> "ha ${minutes / 60} h"
        else -> "ha ${minutes / 1_440} dia(s)"
    }
}

fun Double.format1(): String = ((this * 10).roundToInt() / 10.0).toString()

fun Double.asSignedPercent(): String {
    val v = (this * 10).roundToInt() / 10.0
    return if (v >= 0) "+$v%" else "$v%"
}

fun Double.asPercent(): String = "${(this * 100).roundToInt()}%"

fun Double.isMeaningfulVariation(): Boolean = abs(this) >= 0.1
