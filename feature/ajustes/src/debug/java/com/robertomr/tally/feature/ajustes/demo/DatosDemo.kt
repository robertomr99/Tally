package com.robertomr.tally.feature.ajustes.demo

import com.robertomr.tally.domain.expenses.data.TicketRepository
import com.robertomr.tally.domain.expenses.entities.Ticket
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/*
 * Casos de uso de los datos de demo (spec seed-demo, desviacion D3 confirmada:
 * viven en el source set debug de :feature:ajustes para no existir en release).
 * Sin cambiar DAO ni repositorio: se listan los tickets con un rango amplio y se
 * filtra EN MEMORIA por la marca de demo (`nota`). Nunca se borra por rango ni
 * por comercio.
 */

private val DESDE = LocalDate.of(2000, 1, 1)
private val HASTA = LocalDate.of(2100, 12, 31)

/** Resultado de sembrar: [sembrados] nuevos y [yaEstaban] ya presentes. */
internal data class ResultadoSembrado(val sembrados: Int, val yaEstaban: Int)

/** Tickets de demo (con marca) actuales, en tiempo real. */
internal class ObservarDatosDemo @Inject constructor(
    private val ticketRepository: TicketRepository,
) {
    operator fun invoke(): Flow<List<Ticket>> =
        ticketRepository.observeByDateRange(DESDE, HASTA).map { tickets -> tickets.filter { it.esDemo() } }
}

/**
 * Inserta los tickets de demo que falten. Idempotente por CLAVE (la `nota`
 * exacta), no por fecha: repetir otro dia no duplica y, si falta alguno, repone
 * solo ese.
 */
internal class SembrarDatosDemo @Inject constructor(
    private val ticketRepository: TicketRepository,
) {
    suspend operator fun invoke(hoy: LocalDate): ResultadoSembrado {
        val existentes = ticketRepository.observeByDateRange(DESDE, HASTA).first()
            .mapNotNull { it.nota }
            .toSet()
        var sembrados = 0
        var yaEstaban = 0
        for (ticket in generarTicketsDemo(hoy)) {
            if (ticket.nota in existentes) {
                yaEstaban++
            } else {
                ticketRepository.save(ticket)
                sembrados++
            }
        }
        return ResultadoSembrado(sembrados, yaEstaban)
    }
}

/**
 * Borra EXACTAMENTE los tickets con la marca de demo y devuelve cuantos. Los
 * tickets sin marca (reales) no se tocan: cada borrado se hace por id tras
 * comprobar la marca.
 */
internal class QuitarDatosDemo @Inject constructor(
    private val ticketRepository: TicketRepository,
) {
    suspend operator fun invoke(): Int {
        val demo = ticketRepository.observeByDateRange(DESDE, HASTA).first().filter { it.esDemo() }
        demo.forEach { ticketRepository.delete(it.id) }
        return demo.size
    }
}
