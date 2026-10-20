package com.robertomr.tally.domain.expenses.entities

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

/**
 * Parser/validador COMPARTIDO de las respuestas de los motores de extraccion
 * (spec extraccion-nube; lo reutiliza tal cual `extraccion-local`): de la
 * respuesta cruda del modelo a un [TicketExtraido] validado.
 *
 * Politica de parseo:
 * - **Tolerante en el contenedor**: admite fences ```json, prosa alrededor del
 *   objeto, y numeros con coma decimal espanola (`"3,50"`), claves extra
 *   ignoradas y `cantidad` omitida (= 1).
 * - **Estricto en el schema**: lanza [InvalidExtractionException] con motivo
 *   claro si no hay objeto JSON, el JSON esta roto, `categoria` esta ausente o
 *   fuera de la enum de 6 (match case-insensitive), `fecha_emision` esta
 *   presente pero no es ISO (`yyyy-MM-dd` o `yyyy-MM-dd'T'HH:mm[:ss]`),
 *   `total` esta presente pero no es numerico/∞/negativo, una linea no tiene
 *   `concepto` no-blank, `cantidad` presente es < 1 o no entera, o
 *   `precio_unitario` esta ausente/no numerico/negativo.
 *
 * Regla R3 (docs/domain-model.md): el `total` extraido es SIEMPRE el total CON
 * IVA (Total (impuestos incl.) / TOTAL A PAGAR / TOTAL COMPRA), nunca la base
 * imponible. Aqui solo se valida la forma; la eleccion del importe la hace el
 * motor con su prompt (y `TicketExtractionPrompt` la documenta).
 */
object TicketExtraidoParser {

    /** Json laxo SOLO en claves desconocidas; el resto del schema es estricto. */
    private val json = Json {
        ignoreUnknownKeys = true
    }

    /**
     * @param modelResponse respuesta cruda del modelo (puede traer prosa o
     * fences ```json alrededor del objeto).
     * @param motor motor que produjo la respuesta (queda registrado en el
     * [TicketExtraido] resultante).
     * @throws InvalidExtractionException si la respuesta no cumple el schema.
     */
    fun parse(modelResponse: String, motor: Motor): TicketExtraido {
        val dto = decodificar(extraerObjetoJson(modelResponse))
        return TicketExtraido(
            comercio = dto.comercio?.trim().orEmpty(),
            fechaEmision = dto.fechaEmision?.let { parseFecha(it) },
            total = dto.total?.let { parseTotal(it) },
            lineas = dto.lineas.orEmpty().map { parseLinea(it) },
            categoria = parseCategoria(dto.categoria),
            motor = motor,
            formaPago = FormaPago.desde(dto.formaPago),
            entregado = dto.entregado?.let { parseImporteOpcional(it) },
            cambio = dto.cambio?.let { parseImporteOpcional(it) },
        ).normalizado()
    }

    // --- contenedor tolerante -------------------------------------------------

    /**
     * Quita fences y prosa: se queda del primer `{` al ultimo `}`. Si hay `{`
     * pero no `}` (respuesta truncada), se deja el resto tal cual para que el
     * decodificador lo rechace como "JSON malformado".
     */
    private fun extraerObjetoJson(modelResponse: String): String {
        val sinFences = modelResponse
            .replace("```json", "")
            .replace("```JSON", "")
            .replace("```", "")
            .trim()
        val inicio = sinFences.indexOf('{')
        if (inicio < 0) {
            throw InvalidExtractionException(
                "no se encontro ningun objeto JSON en la respuesta del modelo",
            )
        }
        val fin = sinFences.lastIndexOf('}')
        val finEfectivo = if (fin > inicio) fin + 1 else sinFences.length
        return sinFences.substring(inicio, finEfectivo)
    }

    private fun decodificar(textoJson: String): TicketExtraidoDto = try {
        json.decodeFromString<TicketExtraidoDto>(textoJson)
    } catch (e: SerializationException) {
        throw InvalidExtractionException("JSON malformado en la respuesta del modelo: ${e.message}")
    } catch (e: IllegalArgumentException) {
        throw InvalidExtractionException("JSON malformado en la respuesta del modelo: ${e.message}")
    }

    // --- schema estricto ------------------------------------------------------

    private fun parseCategoria(bruto: String?): Categoria {
        val nombre = bruto?.trim()
            ?: throw InvalidExtractionException("categoria ausente en la respuesta del modelo")
        return Categoria.entries.firstOrNull { it.name.equals(nombre, ignoreCase = true) }
            ?: throw InvalidExtractionException(
                "categoria '$nombre' fuera de la enum de 6 valores " +
                    "(${Categoria.entries.joinToString { it.name }})",
            )
    }

    /** `yyyy-MM-dd` o `yyyy-MM-dd'T'HH:mm[:ss]`; presente pero ilegible = error. */
    private fun parseFecha(bruto: String): LocalDateTime {
        val texto = bruto.trim()
        try {
            return LocalDateTime.parse(texto)
        } catch (_: DateTimeParseException) {
            // cae al intento de fecha sola
        }
        try {
            return LocalDate.parse(texto).atStartOfDay()
        } catch (_: DateTimeParseException) {
            throw InvalidExtractionException(
                "fecha_emision '$texto' no es una fecha ISO " +
                    "(yyyy-MM-dd o yyyy-MM-dd'T'HH:mm[:ss])",
            )
        }
    }

    /** Total con IVA (R3); presente pero no numerico/∞/negativo = error. */
    private fun parseTotal(bruto: JsonElement): Double {
        val valor = parseNumero(bruto, "total")
        if (valor < 0.0) {
            throw InvalidExtractionException("total negativo ($valor): el total con IVA no puede serlo")
        }
        return valor
    }

    /**
     * `entregado`/`cambio` (spec forma-pago) son informativos: si llegan
     * ilegibles, negativos o no finitos se descartan (`null`) en vez de
     * invalidar la extraccion entera; el total sigue siendo lo importante.
     */
    private fun parseImporteOpcional(bruto: JsonElement): Double? {
        val primitivo = bruto as? JsonPrimitive ?: return null
        if (primitivo is JsonNull) return null
        val valor = primitivo.content.aNumero() ?: return null
        return valor.takeIf { it.isFinite() && it >= 0.0 }
    }

    private fun parseLinea(dto: LineaDto): Linea {
        val concepto = dto.concepto?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw InvalidExtractionException("linea sin concepto (concepto ausente o en blanco)")
        val cantidad = parseCantidad(dto.cantidad)
        val precioUnitario = dto.precioUnitario?.let { parseNumero(it, "precio_unitario") }
            ?: throw InvalidExtractionException(
                "precio_unitario ausente en la linea '$concepto' (Linea exige precio unitario)",
            )
        if (precioUnitario < 0.0) {
            throw InvalidExtractionException(
                "precio_unitario negativo ($precioUnitario) en la linea '$concepto'",
            )
        }
        return Linea(concepto = concepto, cantidad = cantidad, precioUnitario = precioUnitario)
    }

    /** Omitida = 1 (default de [Linea]); presente y < 1 o no entera = error. */
    private fun parseCantidad(bruto: JsonElement?): Int {
        if (bruto == null) return 1
        val valor = parseNumero(bruto, "cantidad")
        if (valor < 1.0) {
            throw InvalidExtractionException("cantidad < 1 ($valor): la cantidad minima es 1")
        }
        val entero = valor.toInt()
        if (valor != entero.toDouble()) {
            throw InvalidExtractionException("cantidad no entera ($valor)")
        }
        return entero
    }

    private fun parseNumero(bruto: JsonElement, campo: String): Double {
        val primitivo = bruto as? JsonPrimitive
            ?: throw InvalidExtractionException("$campo no es un valor escalar")
        val texto = primitivo.content.trim()
        val valor = texto.aNumero()
            ?: throw InvalidExtractionException("$campo no es numerico ('$texto')")
        if (!valor.isFinite()) {
            throw InvalidExtractionException("$campo no es finito ('$texto')")
        }
        return valor
    }

    /**
     * Tolera coma decimal espanola en strings (`"3,50"` → 3.5) y miles con punto
     * cuando ambos separadores aparecen (`"1.234,56"` → 1234.56).
     */
    private fun String.aNumero(): Double? {
        val limpio = trim().replace(" ", "")
        if (limpio.isEmpty()) return null
        val normalizado = when {
            limpio.contains(',') && limpio.contains('.') -> limpio.replace(".", "").replace(',', '.')
            limpio.contains(',') -> limpio.replace(',', '.')
            else -> limpio
        }
        return normalizado.toDoubleOrNull()
    }
}

/**
 * DTO interno de parseo (spec extraccion-nube): tolera campos ausentes/null y
 * tipos flexibles (`JsonElement` para numeros, que pueden llegar como string
 * con coma decimal). Los nombres de clave son los del schema del prompt.
 */
@Serializable
internal data class TicketExtraidoDto(
    val comercio: String? = null,
    @SerialName("fecha_emision") val fechaEmision: String? = null,
    val total: JsonElement? = null,
    val categoria: String? = null,
    val lineas: List<LineaDto>? = null,
    @SerialName("forma_pago") val formaPago: String? = null,
    val entregado: JsonElement? = null,
    val cambio: JsonElement? = null,
)

/** DTO interno de una linea del schema (`concepto`, `cantidad`, `precio_unitario`). */
@Serializable
internal data class LineaDto(
    val concepto: String? = null,
    val cantidad: JsonElement? = null,
    @SerialName("precio_unitario") val precioUnitario: JsonElement? = null,
)
