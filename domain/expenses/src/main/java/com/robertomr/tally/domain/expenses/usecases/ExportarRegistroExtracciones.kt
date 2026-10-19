package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.data.RegistroExtracciones
import com.robertomr.tally.domain.expenses.entities.RegistroExtraccion
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Exporta el log retenido como CSV (spec log-extracciones, D4). Solo metadatos:
 * el CSV nunca lleva texto del ticket (D3).
 */
class ExportarRegistroExtracciones @Inject constructor(
    private val registro: RegistroExtracciones,
) {
    suspend operator fun invoke(): String =
        registrosACsv(registro.observarRecientes(RegistroExtracciones.MAXIMO_FILAS).first())
}

internal const val CABECERA_CSV =
    "id,instante_iso,motor,origen,resultado,tipo_fallo,clase_error,duracion_ms,longitud_texto"

/** CSV con cabecera, una fila por registro en el orden recibido y saltos de línea `\n`. */
fun registrosACsv(registros: List<RegistroExtraccion>): String =
    (listOf(CABECERA_CSV) + registros.map { r ->
        listOf(
            r.id.toString(),
            Instant.ofEpochMilli(r.marcaTiempo).toString(),
            r.motor.name,
            r.origen.name,
            r.resultado.name,
            r.tipoFallo?.name.orEmpty(),
            r.claseError.orEmpty(),
            r.duracionMs.toString(),
            r.longitudTexto.toString(),
        ).joinToString(",") { escaparCsv(it) }
    }).joinToString("\n", postfix = "\n")

/** Entrecomilla el campo si lleva coma, comillas o salto de línea (RFC 4180). */
internal fun escaparCsv(campo: String): String =
    if (campo.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"" + campo.replace("\"", "\"\"") + "\""
    } else {
        campo
    }
