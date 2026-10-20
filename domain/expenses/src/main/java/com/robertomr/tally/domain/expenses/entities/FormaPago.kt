package com.robertomr.tally.domain.expenses.entities

import java.text.Normalizer

/**
 * Como se pago un ticket (spec forma-pago, docs/domain-model.md). Es solo
 * informacion: el total (con IVA) sigue siendo lo que cuesta el ticket y lo
 * unico que suman Resumen, donut y listado.
 *
 * - [EFECTIVO]: admite ademas "entregado" y "cambio" (informativos).
 * - [TARJETA]: sin entregado ni cambio.
 * - [NO_INDICADA]: el ticket no lo dice (o es otro medio: bizum, vale, mixto)
 *   y valor de los tickets anteriores a la migracion 2 -> 3.
 */
enum class FormaPago {
    EFECTIVO,
    TARJETA,
    NO_INDICADA,
    ;

    companion object {

        /**
         * Convierte un texto (de la IA o de la BD) en [FormaPago] de forma
         * tolerante: ignora mayusculas, espacios y tildes. Un valor nulo o
         * desconocido (p. ej. "BIZUM") da [NO_INDICADA]; nunca lanza.
         */
        fun desde(valor: String?): FormaPago {
            val limpio = valor?.let { sinTildes(it).trim().uppercase() }.orEmpty()
            return entries.firstOrNull { it.name == limpio } ?: NO_INDICADA
        }

        private fun sinTildes(texto: String): String =
            Normalizer.normalize(texto, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
    }
}
