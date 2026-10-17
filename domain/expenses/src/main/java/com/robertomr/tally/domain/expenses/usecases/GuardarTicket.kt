package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.data.TicketRepository
import com.robertomr.tally.domain.expenses.entities.DatosRevision
import com.robertomr.tally.domain.expenses.entities.Ticket
import com.robertomr.tally.domain.expenses.entities.TicketExtraido
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Mapea el resultado de la extraccion ([TicketExtraido]) mas la cabecera
 * corregida a mano ([DatosRevision]) al agregado [Ticket] y lo persiste (spec
 * revision-datos, "Guardar" del mockup 07).
 *
 * El mapeo vive en dominio (Kotlin puro, testeable en JVM), no en la UI. La
 * escritura multi-tabla (ticket + lineas) la cubre `TicketRepository.save`
 * (`@Transaction` en `TicketDao.upsert`, ya aceptado). La foto ya esta en disco:
 * solo se guarda su ruta relativa ([Ticket.rutaFoto]); el Borrador no se
 * persiste, solo el ticket Guardado.
 *
 * Reglas del mapeo (docs/domain-model.md):
 * - `fechaCaptura` = momento del guardado; el `motor` que extrajo los datos sale
 *   del propio [TicketExtraido].
 * - Las lineas son las de la extraccion, sin editar (solo lectura aqui).
 * - `nota` se persiste `null` (fuera de alcance; "Añadir nota" es non-goal).
 * - `textoCrudoOcr` viaja tal cual desde el Borrador (evidencia del OCR).
 *
 * Una [kotlinx.coroutines.CancellationException] jamas se captura: el caso de
 * uso no envuelve errores y la deja subir al llamador (R-REV4).
 */
class GuardarTicket @Inject constructor(
    private val ticketRepository: TicketRepository,
) {

    /**
     * @param extraido resultado validado del motor (su `motor` y `lineas` se
     * conservan).
     * @param rutaFoto nombre relativo bajo `filesDir` de la foto ya capturada.
     * @param textoCrudo texto crudo del OCR registrado con el Borrador.
     * @param datos cabecera corregida (comercio, fecha, total, categoria).
     * @return id asignado al ticket guardado.
     */
    suspend operator fun invoke(
        extraido: TicketExtraido,
        rutaFoto: String,
        textoCrudo: String,
        datos: DatosRevision,
    ): Long = ticketRepository.save(
        Ticket(
            id = 0L,
            comercio = datos.comercio,
            fechaEmision = datos.fechaEmision,
            total = datos.total,
            categoria = datos.categoria,
            fechaCaptura = LocalDateTime.now(),
            motor = extraido.motor,
            rutaFoto = rutaFoto,
            nota = null,
            textoCrudoOcr = textoCrudo,
            lineas = extraido.lineas,
        ),
    )
}
