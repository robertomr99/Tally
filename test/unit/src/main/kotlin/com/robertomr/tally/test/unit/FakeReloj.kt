package com.robertomr.tally.test.unit

import com.robertomr.tally.domain.expenses.data.Reloj

/** Doble de [Reloj] con avance manual (spec log-extracciones): tests deterministas. */
class FakeReloj(
    var epochMillis: Long = 1_700_000_000_000L,
    var nanos: Long = 0L,
) : Reloj {

    override fun monotonicNanos(): Long = nanos

    override fun epochMillis(): Long = epochMillis

    /** Avanza ambos relojes [ms] milisegundos. */
    fun avanzar(ms: Long) {
        nanos += ms * 1_000_000L
        epochMillis += ms
    }
}
