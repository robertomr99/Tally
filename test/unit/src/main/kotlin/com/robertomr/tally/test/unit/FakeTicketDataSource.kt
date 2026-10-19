package com.robertomr.tally.test.unit

import com.robertomr.tally.domain.expenses.data.TicketDataSource
import com.robertomr.tally.domain.expenses.entities.Ticket
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Doble compartido de [TicketDataSource] (spec persistencia-room).
 *
 * Guarda en memoria con ids autoincrementales y expone la consulta por rango
 * como [Flow] derivado del estado, para que emita al guardar sin volver a
 * consultar (imita el comportamiento de los DAO de Room).
 */
class FakeTicketDataSource : TicketDataSource {

    private val tickets = MutableStateFlow<List<Ticket>>(emptyList())
    private var nextId = 1L

    /** Si se asigna, [save] lo lanza (p. ej. CancellationException). */
    var saveError: Throwable? = null

    override suspend fun save(ticket: Ticket): Long {
        saveError?.let { throw it }
        val id = if (ticket.id == 0L) nextId++ else ticket.id
        val saved = ticket.copy(id = id)
        tickets.update { current -> current.filterNot { it.id == id } + saved }
        return id
    }

    /** Si se asigna, [delete] lo lanza antes de borrar (fallo de BD). */
    var deleteError: Throwable? = null

    /** Si se asigna, [getById] lo lanza (lectura fallida). */
    var getByIdError: Throwable? = null

    override suspend fun delete(id: Long) {
        deleteError?.let { throw it }
        tickets.update { current -> current.filterNot { it.id == id } }
    }

    override suspend fun getById(id: Long): Ticket? {
        getByIdError?.let { throw it }
        return tickets.value.firstOrNull { it.id == id }
    }

    override fun observeByDateRange(from: LocalDate, to: LocalDate): Flow<List<Ticket>> =
        tickets
            .map { current ->
                current
                    .filter {
                        !it.fechaEmision.toLocalDate().isBefore(from) &&
                            !it.fechaEmision.toLocalDate().isAfter(to)
                    }
                    .sortedByDescending { it.fechaEmision }
            }
            .distinctUntilChanged()
}
