package com.robertomr.tally.navigation

// Rutas de navegación con el patrón sealed class de AGENTS.md (no se usa el
// API type-safe con kotlinx-serialization). Solo :app conoce estas rutas.
sealed class TallyRoute(val route: String) {
    data object Resumen : TallyRoute("resumen")
    data object Gastos : TallyRoute("gastos")
    data object Escanear : TallyRoute("escanear")
}
