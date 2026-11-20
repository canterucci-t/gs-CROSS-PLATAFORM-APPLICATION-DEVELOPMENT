package br.com.fiap.tid.data.prefs

import android.content.Context
import br.com.fiap.tid.domain.model.Theme
import br.com.fiap.tid.domain.repository.DataFailureMode

/**
 * Preferencias do app (estado compartilhado que precisa sobreviver ao fim do
 * processo): territorio selecionado, temas monitorados e modo de simulacao.
 *
 * Usamos SharedPreferences por serem suficientes para quatro chaves simples e
 * por nao adicionarem dependencia nova ao projeto. A decisao esta registrada
 * na documentacao (Etapa 3).
 */
class PreferencesStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("tid_prefs", Context.MODE_PRIVATE)

    var selectedTerritoryId: String?
        get() = prefs.getString(KEY_TERRITORY, DEFAULT_TERRITORY)
        set(value) = prefs.edit().putString(KEY_TERRITORY, value).apply()

    var monitoredThemes: Set<String>
        get() = prefs.getStringSet(KEY_THEMES, null) ?: ALL_THEMES
        set(value) = prefs.edit().putStringSet(KEY_THEMES, value).apply()

    var failureMode: DataFailureMode
        get() = runCatching {
            DataFailureMode.valueOf(prefs.getString(KEY_FAILURE, null) ?: "NENHUM")
        }.getOrDefault(DataFailureMode.NENHUM)
        set(value) = prefs.edit().putString(KEY_FAILURE, value.name).apply()

    var responsibleName: String
        get() = prefs.getString(KEY_RESPONSIBLE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_RESPONSIBLE, value).apply()

    fun setThemeMonitored(theme: String, monitored: Boolean) {
        val current = monitoredThemes.toMutableSet()
        if (monitored) current += theme else current -= theme
        // Nunca deixamos a lista vazia: o painel precisa de pelo menos um tema.
        monitoredThemes = if (current.isEmpty()) ALL_THEMES else current
    }

    companion object {
        private const val KEY_TERRITORY = "selected_territory"
        /** Territorio padrao de fabrica, definido pelo curador de dados. */
        const val DEFAULT_TERRITORY = "t-jardim-flores"
        private const val KEY_THEMES = "monitored_themes"
        private const val KEY_FAILURE = "failure_mode"
        private const val KEY_RESPONSIBLE = "responsible_name"
        private val ALL_THEMES: Set<String> = Theme.entries.map { it.name }.toSet()
    }
}
