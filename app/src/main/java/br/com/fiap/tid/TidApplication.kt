package br.com.fiap.tid

import android.app.Application
import android.content.Context
import br.com.fiap.tid.data.local.TidDatabase
import br.com.fiap.tid.data.platform.AndroidAlertNotifier
import br.com.fiap.tid.data.prefs.PreferencesStore
import br.com.fiap.tid.data.repository.TerritoryRepositoryImpl
import br.com.fiap.tid.data.seed.SeedLoader
import br.com.fiap.tid.domain.platform.AlertNotifier
import br.com.fiap.tid.domain.repository.TerritoryRepository
import br.com.fiap.tid.domain.usecase.GetAlertDetailUseCase
import br.com.fiap.tid.domain.usecase.GetDashboardUseCase
import br.com.fiap.tid.domain.usecase.GetIndicatorDetailUseCase
import br.com.fiap.tid.domain.usecase.RegisterDecisionUseCase

/**
 * INJECAO DE DEPENDENCIA MANUAL.
 *
 * Com cinco telas e um repositorio, um container manual resolve o problema sem
 * o custo de build e de aprendizado do Hilt. A decisao esta registrada na
 * documentacao (Etapa 2); trocar por Hilt depois nao afeta dominio nem UI,
 * porque tudo depende de interfaces.
 */
class AppContainer(context: Context) {

    private val database = TidDatabase.get(context)
    private val prefs = PreferencesStore(context)
    private val seedLoader = SeedLoader(context, database.dao())

    val repository: TerritoryRepository =
        TerritoryRepositoryImpl(database.dao(), seedLoader, prefs)

    val notifier: AlertNotifier = AndroidAlertNotifier(context)

    val getDashboard = GetDashboardUseCase(repository)
    val getIndicatorDetail = GetIndicatorDetailUseCase(repository)
    val getAlertDetail = GetAlertDetailUseCase(repository)
    val registerDecision = RegisterDecisionUseCase(repository)

    val preferences: PreferencesStore get() = prefs
}

class TidApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Atalho usado pelos Composables para alcancar o container. */
fun Context.appContainer(): AppContainer =
    (applicationContext as TidApplication).container
