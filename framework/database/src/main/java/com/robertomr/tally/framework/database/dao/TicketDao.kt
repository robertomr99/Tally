package com.robertomr.tally.framework.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.robertomr.tally.framework.database.entity.LineaEntity
import com.robertomr.tally.framework.database.entity.TicketEntity
import com.robertomr.tally.framework.database.entity.TicketWithLineas
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * DAO de tickets y líneas (spec persistencia-room).
 *
 * Las lecturas devuelven [Flow] para que la UI se actualice sola (AGENTS.md).
 * Las escrituras que tocan más de una tabla van con `@Transaction`.
 *
 * La fecha de emisión se compara en epoch millis (codificación de `Converters`);
 * el rango es `[fromMillis, toMillisExclusive)` para cubrir el día `to` entero.
 */
@Dao
interface TicketDao {

    @Insert
    suspend fun insertTicket(ticket: TicketEntity): Long

    @Update
    suspend fun updateTicket(ticket: TicketEntity): Int

    @Insert
    suspend fun insertLineas(lineas: List<LineaEntity>)

    @Query("DELETE FROM lineas WHERE ticket_id = :ticketId")
    suspend fun deleteLineasOfTicket(ticketId: Long)

    /**
     * Borra el ticket; sus líneas desaparecen en cascada por la FK de
     * `lineas`. Devuelve el número de filas borradas (0 si no existía).
     */
    @Query("DELETE FROM tickets WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Transaction
    @Query("SELECT * FROM tickets WHERE id = :id")
    suspend fun getById(id: Long): TicketWithLineas?

    @Transaction
    @Query(
        "SELECT * FROM tickets " +
            "WHERE fecha_emision >= :fromMillis AND fecha_emision < :toMillisExclusive " +
            "ORDER BY fecha_emision DESC"
    )
    fun observeByDateRangeQuery(fromMillis: Long, toMillisExclusive: Long): Flow<List<TicketWithLineas>>

    /**
     * Consulta por rango como [Flow] sin duplicados: la invalidaci��n de Room
     * re-emite resultados iguales al escribir en `tickets` fuera del rango, y a
     * la UI solo le interesa una emisi��n por lista realmente distinta.
     */
    fun observeByDateRange(fromMillis: Long, toMillisExclusive: Long): Flow<List<TicketWithLineas>> =
        observeByDateRangeQuery(fromMillis, toMillisExclusive).distinctUntilChanged()

    /**
     * Guardado transaccional del agregado: upsert del ticket y reemplazo del
     * conjunto de líneas como un todo (una edición reutiliza este método sin
     * dejar líneas antiguas).
     *
     * @return el id del ticket guardado.
     */
    @Transaction
    suspend fun upsert(ticket: TicketEntity, lineas: List<LineaEntity>): Long {
        val id = when {
            ticket.id == 0L -> insertTicket(ticket)
            updateTicket(ticket) > 0 -> ticket.id
            else -> insertTicket(ticket)
        }
        deleteLineasOfTicket(id)
        insertLineas(lineas.map { it.copy(ticketId = id) })
        return id
    }
}
