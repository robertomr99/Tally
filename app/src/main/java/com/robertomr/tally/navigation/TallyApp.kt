package com.robertomr.tally.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.robertomr.tally.feature.common.BottomBarDestino
import com.robertomr.tally.feature.common.TallyBottomBar
import com.robertomr.tally.feature.escanear.EscanearScreen
import com.robertomr.tally.feature.gastos.GastosScreen
import com.robertomr.tally.feature.resumen.ResumenScreen

// Shell de la app: un único NavHost con los 3 destinos de la bottom bar
// (start = Resumen). El NavController solo vive en :app; las pantallas de
// :feature:* reciben callbacks, nunca el NavController.
@Composable
fun TallyApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val activeDestination = backStackEntry.toBottomBarDestino()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            TallyBottomBar(
                activeDestination = activeDestination,
                onNavigate = { destination -> navController.navigateTo(destination) },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TallyRoute.Resumen.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TallyRoute.Resumen.route) { ResumenScreen() }
            composable(TallyRoute.Gastos.route) { GastosScreen() }
            composable(TallyRoute.Escanear.route) {
                // Tras Guardar (spec revision-datos) se vuelve al listado: la
                // pestaña Gastos es el placeholder hasta listado-gastos. Sin
                // ruta nueva; el callback lo cablea :app (la pantalla no recibe
                // el NavController).
                EscanearScreen(
                    onGastoGuardado = { navController.navigateTo(BottomBarDestino.Gastos) },
                )
            }
        }
    }
}

// Navegación estándar de bottom bar: launchSingleTop + restoreState y
// popUpTo(start) con saveState. El back del sistema vuelve a Resumen y un
// segundo back sale de la app.
private fun NavController.navigateTo(destination: BottomBarDestino) {
    navigate(destination.toRoute().route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun BottomBarDestino.toRoute(): TallyRoute = when (this) {
    BottomBarDestino.Resumen -> TallyRoute.Resumen
    BottomBarDestino.Gastos -> TallyRoute.Gastos
    BottomBarDestino.Escanear -> TallyRoute.Escanear
}

private fun NavBackStackEntry?.toBottomBarDestino(): BottomBarDestino =
    when (this?.destination?.route) {
        TallyRoute.Gastos.route -> BottomBarDestino.Gastos
        TallyRoute.Escanear.route -> BottomBarDestino.Escanear
        else -> BottomBarDestino.Resumen
    }
