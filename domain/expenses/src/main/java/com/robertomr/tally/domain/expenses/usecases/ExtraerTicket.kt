package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.data.TextEngine
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.TicketExtraido
import javax.inject.Inject

/**
 * Extrae los datos estructurados del ticket de su texto crudo (spec
 * extraccion-nube, etapa 2 del ADR 0002) con el [TextEngine] disponible.
 *
 * Politica de errores (scenario 5):
 * - Texto blank: NO se invoca el motor (sin gastar cuota) y se devuelve un
 *   [TicketExtraido] vacio con `motor` del motor disponible (la UI lo deja
 *   rellenar a mano).
 * - Cualquier otro error del motor se propaga al llamador SIN resultados
 *   inventados; este caso de uso no captura excepciones. Una
 *   [kotlinx.coroutines.CancellationException] jamas se convierte en resultado.
 *
 * Punto de extensión futuro de `log-extracciones` (envolver este caso de uso).
 */
class ExtraerTicket @Inject constructor(
    private val textEngine: TextEngine,
) {

    /**
     * @param textoCrudo texto tal cual del OCR (etapa 1); `""`/blank →
     * extraccion vacia sin llamar al motor.
     * @return [TicketExtraido] validado con `motor = textEngine.motor`.
     */
    suspend operator fun invoke(textoCrudo: String): TicketExtraido {
        if (textoCrudo.isBlank()) {
            return TicketExtraido(
                comercio = "",
                fechaEmision = null,
                total = null,
                lineas = emptyList(),
                categoria = Categoria.OTROS,
                motor = textEngine.motor,
            )
        }
        return textEngine.extract(textoCrudo)
    }
}
