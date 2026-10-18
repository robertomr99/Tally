package com.robertomr.tally.domain.expenses.entities

/**
 * Se pidio extraer con un motor que no esta [DisponibilidadMotor.DISPONIBLE]
 * (spec reintento-otro-motor, S3). Nunca se invoca el motor ni se cambia a otro.
 */
class MotorNoDisponibleException(
    val motor: Motor,
    val disponibilidad: DisponibilidadMotor,
) : IllegalStateException("El motor $motor no esta disponible: $disponibilidad")
