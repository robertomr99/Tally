package com.robertomr.tally.domain.expenses.entities

import kotlin.math.abs
import kotlin.math.round

/**
 * Reglas puras de la forma de pago (spec forma-pago, docs/domain-model.md).
 * Kotlin puro, testeable en JVM.
 *
 * Invariante persistida: [Ticket.entregado] y [Ticket.cambio] solo son no
 * nulos si la forma es [FormaPago.EFECTIVO]; nunca son negativos ni no finitos.
 * Ninguno de los dos es el total: el total (con IVA) es lo que cuesta.
 */

/** Tolerancia (en euros) de la comprobacion entregado - total = cambio. */
const val TOLERANCIA_PAGO = 0.01

/** Resultado de [normalizarPago]. */
data class PagoNormalizado(
    val formaPago: FormaPago,
    val entregado: Double?,
    val cambio: Double?,
)

/** Aviso NO bloqueante de coherencia del pago en efectivo. */
enum class AvisoPago {
    /** Lo entregado es menor que el total. */
    ENTREGADO_MENOR_QUE_TOTAL,

    /** `entregado - total` no coincide con el cambio (mas de 0,01). */
    CAMBIO_NO_CUADRA,
}

/** Redondeo a centimos. */
internal fun redondearCentimos(valor: Double): Double = round(valor * 100.0) / 100.0

/**
 * Aplica la invariante de pago:
 * 1. entregado o cambio presentes con forma NO_INDICADA -> EFECTIVO (tener
 *    entregado o cambio implica efectivo);
 * 2. forma distinta de EFECTIVO -> entregado = cambio = null;
 * 3. EFECTIVO con total > 0: con entregado -> cambio = entregado - total (si
 *    >= 0, si no `null`; recalculado SIEMPRE, el cambio extraido que lo
 *    contradiga se ignora); solo cambio -> entregado = total + cambio;
 * 4. valores negativos o no finitos -> null.
 *
 * Con TARJETA no se infiere nada de entregado/cambio (se descartan).
 */
fun normalizarPago(
    formaPago: FormaPago,
    total: Double?,
    entregado: Double?,
    cambio: Double?,
): PagoNormalizado {
    var entregadoValido = entregado?.takeIf { it.isFinite() && it >= 0.0 }
    var cambioValido = cambio?.takeIf { it.isFinite() && it >= 0.0 }
    val forma = if (formaPago == FormaPago.NO_INDICADA &&
        (entregadoValido != null || cambioValido != null)
    ) {
        FormaPago.EFECTIVO
    } else {
        formaPago
    }
    if (forma != FormaPago.EFECTIVO) {
        return PagoNormalizado(forma, null, null)
    }
    val totalValido = total?.takeIf { it.isFinite() && it > 0.0 }
    if (totalValido != null) {
        if (entregadoValido != null) {
            // El cambio es derivado (spec gasto-manual-2, A4): se recalcula e ignora
            // el extraido que lo contradiga; `null` si lo entregado no cubre el total.
            val diferencia = redondearCentimos(entregadoValido - totalValido) + 0.0
            cambioValido = if (diferencia >= 0.0) diferencia else null
        } else if (cambioValido != null) {
            entregadoValido = redondearCentimos(totalValido + cambioValido)
        }
    }
    return PagoNormalizado(forma, entregadoValido, cambioValido)
}

/**
 * Version con las invariantes aplicadas; se usa al guardar: con total
 * AUTOMATICO re-sincroniza `total = totalDeLineas` (defensa) y aplica
 * [normalizarPago] (el cambio persistido es siempre el calculado).
 */
fun DatosRevision.normalizada(): DatosRevision {
    val total = if (totalAutomatico && lineas.isNotEmpty()) totalDeLineas(lineas) else total
    val pago = normalizarPago(formaPago, total, entregado, cambio)
    return copy(
        total = total,
        formaPago = pago.formaPago,
        entregado = pago.entregado,
        cambio = pago.cambio,
    )
}

/**
 * Aviso no bloqueante de coherencia (solo con EFECTIVO y entregado rellenado):
 * entregado menor que el total, o cambio que no cuadra con entregado - total.
 */
fun avisoPago(datos: DatosRevision): AvisoPago? {
    if (datos.formaPago != FormaPago.EFECTIVO) return null
    val entregado = datos.entregado ?: return null
    if (entregado < datos.total - TOLERANCIA_PAGO) return AvisoPago.ENTREGADO_MENOR_QUE_TOTAL
    val cambio = datos.cambio ?: return null
    return if (abs(entregado - datos.total - cambio) > TOLERANCIA_PAGO) {
        AvisoPago.CAMBIO_NO_CUADRA
    } else {
        null
    }
}
