package com.robertomr.tally.domain.expenses.entities

/**
 * Si un motor puede usarse ahora mismo para una extraccion explicita (spec
 * reintento-otro-motor, S3). Nube siempre es [DISPONIBLE] (la red no es
 * entrada, D2 de router-automatico); Local requiere capacidad y modelo.
 */
enum class DisponibilidadMotor {
    DISPONIBLE,
    SIN_MODELO,
    NO_CAPAZ,
}
