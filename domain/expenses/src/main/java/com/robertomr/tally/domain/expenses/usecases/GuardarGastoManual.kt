package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.data.TicketRepository
import com.robertomr.tally.domain.expenses.entities.DatosRevision
import com.robertomr.tally.domain.expenses.entities.Ticket
import com.robertomr.tally.domain.expenses.entities.aLineas
import com.robertomr.tally.domain.expenses.entities.errorGastoManual
import com.robertomr.tally.domain.expenses.entities.normalizada
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Persiste un gasto añadido a mano (spec gasto-manual): un [Ticket] sin foto
 * (`rutaFoto = ""`) y sin motor (`motor = null`). No es una extraccion: no pasa
 * por el router ni deja rastro en el log de extracciones.
 *
 * El total es el escrito por el usuario (nunca la suma de lineas) y el pago se
 * normaliza igual que en la revision. Una [kotlinx.coroutines.CancellationException]
 * jamas se captura.
 */
class GuardarGastoManual @Inject constructor(
    private val ticketRepository: TicketRepository,
) {

    /**
     * @param datos formulario del alta; debe cumplir [errorGastoManual] == null.
     * @return id asignado.
     * @throws IllegalArgumentException si los datos no son validos (defensa:
     * la UI ya deshabilita Guardar).
     */
    suspend operator fun invoke(datos: DatosRevision): Long {
        require(datos.errorGastoManual() == null) { "Gasto manual no valido: ${datos.errorGastoManual()}" }
        val pago = datos.normalizada()
        return ticketRepository.save(
            Ticket(
                id = 0L,
                comercio = datos.comercio,
                fechaEmision = datos.fechaEmision,
                total = datos.total,
                categoria = datos.categoria,
                fechaCaptura = LocalDateTime.now(),
                motor = null,
                rutaFoto = "",
                nota = null,
                textoCrudoOcr = null,
                lineas = datos.lineas.aLineas(),
                formaPago = pago.formaPago,
                entregado = pago.entregado,
                cambio = pago.cambio,
            ),
        )
    }
}
