package com.robertomr.tally.domain.expenses.data

import com.robertomr.tally.domain.expenses.entities.DisponibilidadMotor
import com.robertomr.tally.domain.expenses.entities.InvalidExtractionException
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.MotorNoDisponibleException
import com.robertomr.tally.domain.expenses.entities.OrigenExtraccion
import com.robertomr.tally.domain.expenses.entities.RegistroExtraccion
import com.robertomr.tally.domain.expenses.entities.ResultadoExtraccion
import com.robertomr.tally.domain.expenses.entities.TicketExtraido
import com.robertomr.tally.domain.expenses.entities.TipoFallo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Decorador transparente sobre el router (spec log-extracciones, D2): registra
 * una fila de metadatos por cada llamada real a un motor, por las dos rutas
 * (`extract(texto)` = ROUTER, `extract(motor, texto)` = REINTENTO), sin alterar
 * resultado ni errores.
 *
 * - [delegado] y [porMotor] son, en producción, la misma instancia del router.
 * - Texto blank: los casos de uso cortocircuitan antes de llegar aquí, así que
 *   no se registra nada.
 * - La [CancellationException] se registra como CANCELADA (escritura en
 *   [NonCancellable]) y SIEMPRE se relanza.
 * - Un fallo al escribir el log se ignora (D6): nunca oculta la extracción.
 * - Privacidad (D3): solo se guarda la longitud del texto y la clase del error.
 */
class EngineConRegistro(
    private val delegado: TextEngine,
    private val porMotor: ExtractorPorMotor,
    private val registro: RegistroExtracciones,
    private val reloj: Reloj,
) : TextEngine, ExtractorPorMotor {

    override val motor: Motor get() = delegado.motor

    override fun disponibilidad(motor: Motor): DisponibilidadMotor = porMotor.disponibilidad(motor)

    override suspend fun extract(textoCrudo: String): TicketExtraido =
        medir(delegado.motor, OrigenExtraccion.ROUTER, textoCrudo.length) {
            delegado.extract(textoCrudo)
        }

    override suspend fun extract(motor: Motor, textoCrudo: String): TicketExtraido =
        medir(motor, OrigenExtraccion.REINTENTO, textoCrudo.length) {
            porMotor.extract(motor, textoCrudo)
        }

    private suspend fun medir(
        motor: Motor,
        origen: OrigenExtraccion,
        longitudTexto: Int,
        bloque: suspend () -> TicketExtraido,
    ): TicketExtraido {
        val marca = reloj.epochMillis()
        val inicio = reloj.monotonicNanos()
        fun duracionMs() = (reloj.monotonicNanos() - inicio) / NANOS_POR_MS

        try {
            val resultado = bloque()
            anotar(
                RegistroExtraccion(
                    marcaTiempo = marca,
                    motor = motor,
                    origen = origen,
                    resultado = ResultadoExtraccion.EXITO,
                    duracionMs = duracionMs(),
                    longitudTexto = longitudTexto,
                ),
            )
            return resultado
        } catch (e: CancellationException) {
            anotar(
                RegistroExtraccion(
                    marcaTiempo = marca,
                    motor = motor,
                    origen = origen,
                    resultado = ResultadoExtraccion.CANCELADA,
                    duracionMs = duracionMs(),
                    longitudTexto = longitudTexto,
                ),
            )
            throw e
        } catch (e: Exception) {
            anotar(
                RegistroExtraccion(
                    marcaTiempo = marca,
                    motor = (e as? MotorNoDisponibleException)?.motor ?: motor,
                    origen = origen,
                    resultado = ResultadoExtraccion.FALLO,
                    tipoFallo = tipoFallo(e),
                    claseError = e::class.simpleName ?: "Desconocida",
                    duracionMs = duracionMs(),
                    longitudTexto = longitudTexto,
                ),
            )
            throw e
        }
    }

    /**
     * Escribe en [NonCancellable] para que la fila de una extracción cancelada
     * no se pierda, y traga cualquier fallo del log que no sea cancelación.
     */
    private suspend fun anotar(fila: RegistroExtraccion) {
        withContext(NonCancellable) {
            try {
                registro.registrar(fila)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // El log es best-effort: no altera la extracción (escenario 7).
            }
        }
    }

    private fun tipoFallo(e: Exception): TipoFallo = when (e) {
        is InvalidExtractionException -> TipoFallo.RESPUESTA_INVALIDA
        is MotorNoDisponibleException -> TipoFallo.MOTOR_NO_DISPONIBLE
        else -> TipoFallo.OTRO
    }

    private companion object {
        const val NANOS_POR_MS = 1_000_000L
    }
}
