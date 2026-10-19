package com.robertomr.tally.test.unit

import com.robertomr.tally.domain.expenses.data.RegistroExtracciones
import com.robertomr.tally.domain.expenses.entities.RegistroExtraccion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Doble compartido de [RegistroExtracciones] (spec log-extracciones): guarda las
 * filas en memoria (la más reciente primero, con ids autoincrementales) y
 * permite forzar que [registrar] falle con [errorAlRegistrar].
 */
class FakeRegistroExtracciones : RegistroExtracciones {

    private val filas = MutableStateFlow<List<RegistroExtraccion>>(emptyList())

    /** Si se asigna, [registrar] lanza este error en lugar de guardar. */
    var errorAlRegistrar: Throwable? = null

    /** Filas guardadas, de la más antigua a la más reciente (orden de inserción). */
    val registrados: List<RegistroExtraccion> get() = filas.value.asReversed()

    override suspend fun registrar(registro: RegistroExtraccion) {
        errorAlRegistrar?.let { throw it }
        val id = (filas.value.maxOfOrNull { it.id } ?: 0L) + 1
        filas.value = listOf(registro.copy(id = id)) + filas.value
    }

    override fun observarRecientes(limite: Int): Flow<List<RegistroExtraccion>> =
        filas.map { it.take(limite) }

    override suspend fun borrarTodo() {
        filas.value = emptyList()
    }
}
