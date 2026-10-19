package com.robertomr.tally.domain.expenses.data

import com.robertomr.tally.domain.expenses.entities.RegistroExtraccion
import kotlinx.coroutines.flow.Flow

/**
 * Registro local de extracciones (spec log-extracciones; implementación Room en
 * `:framework:database`). Solo metadatos (D3).
 */
interface RegistroExtracciones {

    /**
     * Añade [registro] y recorta el log a las [MAXIMO_FILAS] más recientes en la
     * misma transacción (retención).
     */
    suspend fun registrar(registro: RegistroExtraccion)

    /** Los [limite] registros más recientes, del más nuevo al más antiguo. */
    fun observarRecientes(limite: Int): Flow<List<RegistroExtraccion>>

    suspend fun borrarTodo()

    companion object {
        /** Retención del log (spec log-extracciones, D3). */
        const val MAXIMO_FILAS = 500
    }
}
