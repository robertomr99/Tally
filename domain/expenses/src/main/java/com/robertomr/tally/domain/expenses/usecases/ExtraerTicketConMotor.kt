package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.data.ExtractorPorMotor
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.TicketExtraido
import javax.inject.Inject

/**
 * Extrae el ticket con un [Motor] concreto (spec reintento-otro-motor). Misma
 * politica que `ExtraerTicket`: texto blank -> extraccion vacia con ese motor
 * sin invocarlo; cualquier error (y la cancelacion) se propaga sin capturar.
 */
class ExtraerTicketConMotor @Inject constructor(
    private val extractor: ExtractorPorMotor,
) {
    suspend operator fun invoke(textoCrudo: String, motor: Motor): TicketExtraido {
        if (textoCrudo.isBlank()) {
            return TicketExtraido(
                comercio = "",
                fechaEmision = null,
                total = null,
                lineas = emptyList(),
                categoria = Categoria.OTROS,
                motor = motor,
            )
        }
        return extractor.extract(motor, textoCrudo)
    }
}
