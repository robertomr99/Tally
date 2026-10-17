package com.robertomr.tally.test.unit

import com.robertomr.tally.domain.expenses.data.TextEngine
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.TicketExtraido
import kotlinx.coroutines.CompletableDeferred

/**
 * Doble compartido de [TextEngine] (spec extraccion-nube): registra las
 * invocaciones (textos crudos recibidos) y devuelve un [TicketExtraido]
 * configurable, o lanza un error configurable. [gate] permite retener la
 * extraccion para observar estados intermedios o cancelar en tests.
 */
class FakeTextEngine(
    var result: TicketExtraido? = null,
    var error: Throwable? = null,
    override val motor: Motor = Motor.NUBE,
) : TextEngine {

    val received = mutableListOf<String>()

    /** Si se asigna, [extract] espera a que se complete antes de responder. */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun extract(textoCrudo: String): TicketExtraido {
        received += textoCrudo
        gate?.await()
        error?.let { throw it }
        return result ?: TicketExtraido(
            comercio = "COMERCIO FAKE",
            fechaEmision = null,
            total = 1.0,
            lineas = emptyList(),
            categoria = Categoria.OTROS,
            motor = motor,
        )
    }
}
