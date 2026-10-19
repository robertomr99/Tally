package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.data.RegistroExtracciones
import com.robertomr.tally.domain.expenses.entities.DiagnosticoExtracciones
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.RegistroExtraccion
import com.robertomr.tally.domain.expenses.entities.ResultadoExtraccion
import com.robertomr.tally.domain.expenses.entities.ResumenMotor
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Consulta de diagnóstico (spec log-extracciones, D4): resumen por motor sobre
 * todo el log retenido y las últimas [LIMITE_RECIENTES] entradas.
 */
class ObservarRegistroExtracciones @Inject constructor(
    private val registro: RegistroExtracciones,
) {
    operator fun invoke(): Flow<DiagnosticoExtracciones> =
        registro.observarRecientes(RegistroExtracciones.MAXIMO_FILAS).map { todos ->
            DiagnosticoExtracciones(
                resumen = resumirPorMotor(todos),
                recientes = todos.take(LIMITE_RECIENTES),
            )
        }

    companion object {
        const val LIMITE_RECIENTES = 20
    }
}

/** Resumen por motor (solo motores con al menos una fila), en el orden de [Motor]. */
fun resumirPorMotor(registros: List<RegistroExtraccion>): List<ResumenMotor> =
    Motor.entries.mapNotNull { motor ->
        val delMotor = registros.filter { it.motor == motor }
        if (delMotor.isEmpty()) return@mapNotNull null
        val exitos = delMotor.filter { it.resultado == ResultadoExtraccion.EXITO }
        ResumenMotor(
            motor = motor,
            total = delMotor.size,
            exitos = exitos.size,
            duracionMediaExitoMs = exitos.takeIf { it.isNotEmpty() }
                ?.let { lista -> lista.sumOf { it.duracionMs } / lista.size },
        )
    }
