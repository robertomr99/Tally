package com.robertomr.tally.domain.expenses.entities

/** El otro motor ofrecido para reintentar y si se puede usar ahora (spec reintento-otro-motor). */
data class OfertaMotor(
    val motor: Motor,
    val disponibilidad: DisponibilidadMotor,
)
