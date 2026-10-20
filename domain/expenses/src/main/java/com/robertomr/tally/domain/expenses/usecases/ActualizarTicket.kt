package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.data.TicketRepository
import com.robertomr.tally.domain.expenses.entities.DatosRevision
import com.robertomr.tally.domain.expenses.entities.Ticket
import com.robertomr.tally.domain.expenses.entities.aLineas
import com.robertomr.tally.domain.expenses.entities.normalizada
import javax.inject.Inject

/**
 * "Guardar cambios" del detalle de un ticket ya guardado (spec detalle-ticket):
 * aplica la cabecera y las lineas editadas ([DatosRevision]) sobre el
 * [Ticket] original y lo persiste con el MISMO id (`TicketRepository.save`, que
 * el DAO trata como update + reemplazo de lineas).
 *
 * Nunca toca `id`, `rutaFoto`, `fechaCaptura`, `motor`, `textoCrudoOcr` ni
 * `nota`. El total es el de [DatosRevision] (autoritativo), jamas la suma de
 * lineas. Una `CancellationException` sube al llamador sin capturarse.
 */
class ActualizarTicket @Inject constructor(
    private val ticketRepository: TicketRepository,
) {

    /** @return id del ticket actualizado (el mismo que [original]). */
    suspend operator fun invoke(original: Ticket, datos: DatosRevision): Long {
        // Forma de pago (spec forma-pago): normalizada al guardar; no altera el total.
        val pago = datos.normalizada()
        return ticketRepository.save(
            original.copy(
                comercio = datos.comercio,
                fechaEmision = datos.fechaEmision,
                total = datos.total,
                categoria = datos.categoria,
                lineas = datos.lineas.aLineas(),
                formaPago = pago.formaPago,
                entregado = pago.entregado,
                cambio = pago.cambio,
            ),
        )
    }
}
