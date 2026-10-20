package com.robertomr.tally.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.robertomr.tally.feature.ajustes.AjustesScreen
import com.robertomr.tally.feature.common.BottomBarDestino
import com.robertomr.tally.feature.common.TallyBottomBar
import com.robertomr.tally.feature.escanear.EscanearScreen
import com.robertomr.tally.feature.gastos.GastosScreen
import com.robertomr.tally.feature.gastos.TicketDetalleScreen
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
            // El detalle del ticket (spec detalle-ticket, D5) oculta la barra
            // inferior: su dock de edicion ya ocupa ese borde.
            if (backStackEntry?.destination?.route != TallyRoute.Ticket.route) {
                TallyBottomBar(
                    activeDestination = activeDestination,
                    onNavigate = { destination -> navController.navigateTo(destination) },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TallyRoute.Resumen.route,
            // Un solo inset superior: el Scaffold ya reserva la barra de estado en
            // innerPadding y se marca como consumido; así los statusBarsPadding() de
            // cada pantalla resuelven a 0 en vez de duplicar el hueco (V01). La
            // altura de la barra de estado la aporta innerPadding.
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            // Entre pestañas: fundido corto. Hacia/desde las pantallas secundarias
            // (Detalle, Ajustes): deslizamiento horizontal con pop inverso (V19).
            enterTransition = {
                if (targetState.esSecundaria()) {
                    slideInHorizontally(tween(DURACION_DESLIZAMIENTO_MS)) { ancho -> ancho }
                } else {
                    fadeIn(tween(DURACION_FUNDIDO_MS))
                }
            },
            exitTransition = {
                if (targetState.esSecundaria()) {
                    slideOutHorizontally(tween(DURACION_DESLIZAMIENTO_MS)) { ancho -> -ancho / 4 } +
                        fadeOut(tween(DURACION_DESLIZAMIENTO_MS))
                } else {
                    fadeOut(tween(DURACION_FUNDIDO_MS))
                }
            },
            popEnterTransition = {
                if (initialState.esSecundaria()) {
                    slideInHorizontally(tween(DURACION_DESLIZAMIENTO_MS)) { ancho -> -ancho / 4 } +
                        fadeIn(tween(DURACION_DESLIZAMIENTO_MS))
                } else {
                    fadeIn(tween(DURACION_FUNDIDO_MS))
                }
            },
            popExitTransition = {
                if (initialState.esSecundaria()) {
                    slideOutHorizontally(tween(DURACION_DESLIZAMIENTO_MS)) { ancho -> ancho }
                } else {
                    fadeOut(tween(DURACION_FUNDIDO_MS))
                }
            },
        ) {
            composable(TallyRoute.Resumen.route) {
                ResumenScreen(
                    onAbrirAjustes = { navController.navigate(TallyRoute.Ajustes.route) },
                    onTicketClick = { id -> navController.navigate(TallyRoute.Ticket.crear(id)) },
                    onVerTodos = { navController.navigateTo(BottomBarDestino.Gastos) },
                )
            }
            composable(
                route = TallyRoute.Ticket.route,
                arguments = listOf(navArgument(TallyRoute.Ticket.ARG_ID) { type = NavType.LongType }),
            ) {
                // El id llega al ViewModel por SavedStateHandle (clave = ARG_ID).
                TicketDetalleScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(TallyRoute.Ajustes.route) {
                AjustesScreen(onBack = { navController.popBackStack() })
            }
            composable(TallyRoute.Gastos.route) {
                GastosScreen(
                    onTicketClick = { id -> navController.navigate(TallyRoute.Ticket.crear(id)) },
                    onEscanear = { navController.navigateTo(BottomBarDestino.Escanear) },
                )
            }
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

private const val DURACION_FUNDIDO_MS = 150
private const val DURACION_DESLIZAMIENTO_MS = 300

// Detalle del ticket y Ajustes cuelgan de una pestaña: se abren deslizando.
private fun NavBackStackEntry.esSecundaria(): Boolean =
    destination.route == TallyRoute.Ticket.route || destination.route == TallyRoute.Ajustes.route

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
