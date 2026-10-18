package com.robertomr.tally.domain.expenses.entities

/**
 * Métricas de la tarjeta de total. Los importes se suman en `Double` y se
 * redondean solo al formatear. [variacion] es una fracción (0,5125 = +51,25 %)
 * y es `null` cuando el período anterior no tiene gasto (sin referencia).
 */
data class ResumenPeriodo(
    val total: Double,
    val numTickets: Int,
    val promedioDiario: Double,
    val totalAnterior: Double,
    val variacion: Double?,
)
