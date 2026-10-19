package com.robertomr.tally.framework.database.source

import com.robertomr.tally.domain.expenses.data.Reloj

/** [Reloj] real: `System.nanoTime` para duraciones y `currentTimeMillis` para la marca. */
internal class RelojSistema : Reloj {

    override fun monotonicNanos(): Long = System.nanoTime()

    override fun epochMillis(): Long = System.currentTimeMillis()
}
