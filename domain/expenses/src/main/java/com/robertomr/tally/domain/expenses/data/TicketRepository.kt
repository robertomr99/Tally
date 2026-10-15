package com.robertomr.tally.domain.expenses.data

import com.robertomr.tally.domain.expenses.entities.Ticket
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio de tickets: API pública de la capa de persistencia (spec
 * persistencia-room). Clase concreta sobre las interfaces de DataSource
 * (AGENTS.md); sin dependencias técnicas.
 *
 * Al borrar, el orden es DB-antes-que-foto: si falla el borrado del fichero
 * queda un huérfano en disco, preferible a borrar la foto y perder el ticket.
 */
class TicketRepository @Inject constructor(
    private val ticketDataSource: TicketDataSource,
    private val photoFileStore: PhotoFileStore,
) {

    suspend fun save(ticket: Ticket): Long = ticketDataSource.save(ticket)

    /**
     * Borra el ticket, sus líneas (cascada en la fuente de datos) y su foto en
     * disco (best-effort). Un id inexistente es un no-op.
     */
    suspend fun delete(id: Long) {
        val ticket = ticketDataSource.getById(id) ?: return
        ticketDataSource.delete(id)
        photoFileStore.delete(ticket.rutaFoto)
    }

    suspend fun getById(id: Long): Ticket? = ticketDataSource.getById(id)

    fun observeByDateRange(from: LocalDate, to: LocalDate): Flow<List<Ticket>> =
        ticketDataSource.observeByDateRange(from, to)
}
