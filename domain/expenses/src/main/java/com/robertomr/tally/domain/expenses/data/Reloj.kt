package com.robertomr.tally.domain.expenses.data

/**
 * Reloj inyectable (spec log-extracciones, D5) para medir duraciones y marcar
 * instantes de forma determinista en los tests.
 */
interface Reloj {

    /** Tiempo monótono en nanosegundos; solo sirve para restar (duraciones). */
    fun monotonicNanos(): Long

    /** Instante actual en epoch millis (marca de tiempo del registro). */
    fun epochMillis(): Long
}
