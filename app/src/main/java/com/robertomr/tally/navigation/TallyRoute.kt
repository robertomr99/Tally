package com.robertomr.tally.navigation

// Rutas de navegación con el patrón sealed class de AGENTS.md (no se usa el
// API type-safe con kotlinx-serialization). Solo :app conoce estas rutas.
sealed class TallyRoute(val route: String) {
    data object Resumen : TallyRoute("resumen")
    data object Gastos : TallyRoute("gastos")
    data object Escanear : TallyRoute("escanear")

    // Pantalla secundaria (sin pestaña de la bottom bar): se abre desde Resumen.
    data object Ajustes : TallyRoute("ajustes")

    // Alta de un gasto sin ticket (spec gasto-manual), abierta desde Gastos.
    data object NuevoGasto : TallyRoute("gasto/nuevo")

    // Detalle del ticket guardado (spec detalle-ticket). ARG_ID debe coincidir con
    // TicketDetalleViewModel.ARG_ID: el ViewModel lo lee de su SavedStateHandle.
    data object Ticket : TallyRoute("ticket/{ticketId}") {
        const val ARG_ID = "ticketId"
        fun crear(id: Long): String = "ticket/$id"
    }
}
