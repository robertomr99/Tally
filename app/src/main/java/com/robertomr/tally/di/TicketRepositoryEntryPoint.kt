package com.robertomr.tally.di

import com.robertomr.tally.domain.expenses.data.TicketRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * EntryPoint del [TicketRepository] para el E2E de periodo-selector: el test
 * instrumentado siembra (y limpia) tickets con el repositorio real del grafo
 * (mismo patrón y motivo que [TextEngineEntryPoint]: debe vivir en main).
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface TicketRepositoryEntryPoint {
    fun ticketRepository(): TicketRepository
}
