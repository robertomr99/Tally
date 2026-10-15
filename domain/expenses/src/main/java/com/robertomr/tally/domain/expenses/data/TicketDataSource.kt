package com.robertomr.tally.domain.expenses.data

import com.robertomr.tally.domain.expenses.entities.Ticket
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * Fuente de datos de tickets (interfaz de dominio; la implementación Room vive
 * en `:framework:database`).
 *
 * Las consultas devuelven [Flow] para que la UI se actualice sola (AGENTS.md).
 * El rango de fechas es inclusivo y filtra por fecha de emisión, no de captura
 * (docs/domain-model.md).
 */
interface TicketDataSource {

    /**
     * Guarda o actualiza el ticket y reemplaza su conjunto de líneas como un
     * todo, en una única transacción (una edición futura reutiliza este método).
     *
     * Un [Ticket.id] igual a 0 crea un ticket nuevo; si no, se actualiza el
     * existente con ese id (o se inserta con ese mismo id si ya no estuviera).
     *
     * @return el id del ticket guardado.
     */
    suspend fun save(ticket: Ticket): Long

    /**
     * Borra el ticket con sus líneas (cascada). Un id inexistente es un no-op.
     */
    suspend fun delete(id: Long)

    suspend fun getById(id: Long): Ticket?

    /**
     * Tickets con fecha de emisión en el rango `[from, to]` (ambos inclusive),
     * ordenados de más reciente a más antiguo.
     */
    fun observeByDateRange(from: LocalDate, to: LocalDate): Flow<List<Ticket>>
}
