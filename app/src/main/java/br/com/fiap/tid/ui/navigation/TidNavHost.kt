package br.com.fiap.tid.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.fiap.tid.ui.screens.alert.AlertScreen
import br.com.fiap.tid.ui.screens.dashboard.DashboardScreen
import br.com.fiap.tid.ui.screens.decisions.DecisionsScreen
import br.com.fiap.tid.ui.screens.indicator.IndicatorScreen
import br.com.fiap.tid.ui.screens.settings.SettingsScreen

/**
 * MAPA DE ROTAS.
 *
 * Regra do projeto: a rota transporta apenas IDENTIFICADORES, nunca o objeto
 * inteiro. Quem precisa do dado recarrega do repositorio, o que mantem o
 * estado consistente ao voltar e sobrevive a morte do processo.
 */
object Routes {
    const val DASHBOARD = "dashboard"
    const val INDICATOR = "indicator/{territoryId}/{indicatorId}"
    const val ALERT = "alert/{territoryId}/{indicatorId}"
    const val DECISIONS = "decisions"
    const val SETTINGS = "settings"

    fun indicator(territoryId: String, indicatorId: String) = "indicator/$territoryId/$indicatorId"
    fun alert(territoryId: String, indicatorId: String) = "alert/$territoryId/$indicatorId"
}

private val idArgs = listOf(
    navArgument("territoryId") { type = NavType.StringType },
    navArgument("indicatorId") { type = NavType.StringType }
)

@Composable
fun TidNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.DASHBOARD) {

        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onOpenIndicator = { t, i -> navController.navigate(Routes.indicator(t, i)) },
                onOpenAlert = { t, i -> navController.navigate(Routes.alert(t, i)) },
                onOpenDecisions = { navController.navigate(Routes.DECISIONS) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.INDICATOR, arguments = idArgs) { entry ->
            IndicatorScreen(
                territoryId = entry.arguments?.getString("territoryId").orEmpty(),
                indicatorId = entry.arguments?.getString("indicatorId").orEmpty(),
                onBack = { navController.popBackStack() },
                onOpenAlert = { t, i -> navController.navigate(Routes.alert(t, i)) }
            )
        }

        composable(Routes.ALERT, arguments = idArgs) { entry ->
            AlertScreen(
                territoryId = entry.arguments?.getString("territoryId").orEmpty(),
                indicatorId = entry.arguments?.getString("indicatorId").orEmpty(),
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.DECISIONS) {
            DecisionsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
