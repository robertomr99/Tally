package com.robertomr.tally.domain.expenses.data

import com.robertomr.tally.domain.expenses.entities.DisponibilidadMotor
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.TicketExtraido

/**
 * Extraccion explicita con un motor concreto (spec reintento-otro-motor): el
 * reintento "con el otro motor" ignora la politica del router y la preferencia.
 * Sin fallback: un motor no disponible lanza `MotorNoDisponibleException`.
 */
interface ExtractorPorMotor {

    /** Disponibilidad actual de [motor] para una extraccion explicita. */
    fun disponibilidad(motor: Motor): DisponibilidadMotor

    /**
     * Extrae [textoCrudo] con [motor].
     * @throws com.robertomr.tally.domain.expenses.entities.MotorNoDisponibleException
     * si [disponibilidad] no es `DISPONIBLE` (el motor no se invoca).
     */
    suspend fun extract(motor: Motor, textoCrudo: String): TicketExtraido
}
