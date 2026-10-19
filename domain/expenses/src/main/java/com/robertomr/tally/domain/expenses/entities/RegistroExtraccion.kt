package com.robertomr.tally.domain.expenses.entities

/** Por qué ruta se invocó el motor (spec log-extracciones, D2). */
enum class OrigenExtraccion {
    /** `TextEngine.extract(texto)`: el router elige el motor según la preferencia. */
    ROUTER,

    /** `ExtractorPorMotor.extract(motor, texto)`: reintento explícito del usuario. */
    REINTENTO,
}

/** Cómo terminó la llamada al motor. */
enum class ResultadoExtraccion {
    EXITO,
    FALLO,
    CANCELADA,
}

/** Clasificación gruesa del fallo; nunca el mensaje de la excepción (privacidad, D3). */
enum class TipoFallo {
    RESPUESTA_INVALIDA,
    MOTOR_NO_DISPONIBLE,
    OTRO,
}

/**
 * Metadatos de UNA llamada a un motor de extracción (spec log-extracciones).
 *
 * Privacidad (D3): solo metadatos. Jamás el texto crudo (solo su longitud),
 * campos del ticket, mensajes de excepción ni credenciales.
 *
 * @property marcaTiempo inicio de la llamada, epoch millis.
 * @property claseError `simpleName` de la excepción (nunca su `message`).
 * @property duracionMs duración medida con el [com.robertomr.tally.domain.expenses.data.Reloj].
 * @property longitudTexto `texto.length` del texto crudo enviado al motor.
 */
data class RegistroExtraccion(
    val id: Long = 0L,
    val marcaTiempo: Long,
    val motor: Motor,
    val origen: OrigenExtraccion,
    val resultado: ResultadoExtraccion,
    val tipoFallo: TipoFallo? = null,
    val claseError: String? = null,
    val duracionMs: Long,
    val longitudTexto: Int,
)

/** Resumen por motor para la consulta de diagnóstico (n, éxitos, duración media de los éxitos). */
data class ResumenMotor(
    val motor: Motor,
    val total: Int,
    val exitos: Int,
    /** Media de [RegistroExtraccion.duracionMs] de los éxitos; `null` si no hay ninguno. */
    val duracionMediaExitoMs: Long?,
)

/** Contenido de la consulta de diagnóstico: resumen sobre todo el log y últimas entradas. */
data class DiagnosticoExtracciones(
    val resumen: List<ResumenMotor>,
    val recientes: List<RegistroExtraccion>,
)
