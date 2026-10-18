package com.robertomr.tally.domain.expenses.data

import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.PreferenciaMotor
import com.robertomr.tally.domain.expenses.entities.TicketExtraido

/**
 * Politica pura de seleccion de motor (ADR 0001, spec router-automatico D1):
 * - [PreferenciaMotor.NUBE] -> siempre Nube.
 * - [PreferenciaMotor.LOCAL] -> siempre Local, sin comprobar capacidad ni modelo
 *   (si falta el modelo, el motor local lanza `ModeloLocalNoDisponibleException`).
 * - [PreferenciaMotor.AUTO] -> Local solo si [capaz] y [modeloDisponible]; si no, Nube.
 */
fun elegirMotor(
    preferencia: PreferenciaMotor,
    capaz: Boolean,
    modeloDisponible: Boolean,
): Motor = when (preferencia) {
    PreferenciaMotor.NUBE -> Motor.NUBE
    PreferenciaMotor.LOCAL -> Motor.LOCAL
    PreferenciaMotor.AUTO -> if (capaz && modeloDisponible) Motor.LOCAL else Motor.NUBE
}

/**
 * [TextEngine] que elige por extraccion entre el motor local y el de nube y
 * delega. NO hay fallback: si el motor elegido falla, el error (y la
 * cancelacion) se propaga tal cual; reintentar con el otro motor es una
 * accion explicita del usuario (ADR 0001).
 */
class EngineRouter(
    private val local: TextEngine,
    private val nube: TextEngine,
    private val capacidad: CapacidadDispositivo,
    private val modelDownloader: ModelDownloader,
    private val preferencia: PreferenciaMotorSource,
) : TextEngine {

    private fun elegido(): TextEngine {
        val motor = elegirMotor(
            preferencia = preferencia.actual(),
            capaz = capacidad.puedeEjecutarModeloLocal(),
            modeloDisponible = modelDownloader.rutaModelo() != null,
        )
        return when (motor) {
            Motor.LOCAL -> local
            Motor.NUBE -> nube
        }
    }

    override val motor: Motor get() = elegido().motor

    override suspend fun extract(textoCrudo: String): TicketExtraido =
        elegido().extract(textoCrudo)
}
