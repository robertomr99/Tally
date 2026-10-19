package com.robertomr.tally.framework.database.source

import com.robertomr.tally.domain.expenses.data.RegistroExtracciones
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.OrigenExtraccion
import com.robertomr.tally.domain.expenses.entities.RegistroExtraccion
import com.robertomr.tally.domain.expenses.entities.ResultadoExtraccion
import com.robertomr.tally.domain.expenses.entities.TipoFallo
import com.robertomr.tally.framework.database.dao.ExtraccionLogDao
import com.robertomr.tally.framework.database.entity.ExtraccionLogEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementación Room de [RegistroExtracciones] (interna de `:framework:database`;
 * AGENTS.md). Aplica la retención de [RegistroExtracciones.MAXIMO_FILAS] filas en
 * la misma transacción que la inserción.
 */
internal class RegistroExtraccionesLocalDataSource(
    private val dao: ExtraccionLogDao,
    private val maximoFilas: Int = RegistroExtracciones.MAXIMO_FILAS,
) : RegistroExtracciones {

    override suspend fun registrar(registro: RegistroExtraccion) {
        dao.insertarYRecortar(registro.toEntity(), maximoFilas)
    }

    override fun observarRecientes(limite: Int): Flow<List<RegistroExtraccion>> =
        dao.observarRecientes(limite).map { filas -> filas.map { it.toDomain() } }

    override suspend fun borrarTodo() = dao.borrarTodo()
}

private fun RegistroExtraccion.toEntity() = ExtraccionLogEntity(
    id = id,
    marcaTiempo = marcaTiempo,
    motor = motor.name,
    origen = origen.name,
    resultado = resultado.name,
    tipoFallo = tipoFallo?.name,
    claseError = claseError,
    duracionMs = duracionMs,
    longitudTexto = longitudTexto,
)

private fun ExtraccionLogEntity.toDomain() = RegistroExtraccion(
    id = id,
    marcaTiempo = marcaTiempo,
    motor = Motor.valueOf(motor),
    origen = OrigenExtraccion.valueOf(origen),
    resultado = ResultadoExtraccion.valueOf(resultado),
    tipoFallo = tipoFallo?.let { TipoFallo.valueOf(it) },
    claseError = claseError,
    duracionMs = duracionMs,
    longitudTexto = longitudTexto,
)
