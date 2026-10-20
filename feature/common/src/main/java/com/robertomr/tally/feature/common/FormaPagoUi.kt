package com.robertomr.tally.feature.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.robertomr.tally.domain.expenses.entities.FormaPago

/*
 * Presentacion de la forma de pago (spec forma-pago, DESIGN.md "Forma de pago"):
 * etiqueta, icono y colores compartidos por el selector de la revision/detalle,
 * el papel de solo lectura, el chip del listado y el chip del detalle.
 *
 * La diferencia efectivo/tarjeta NUNCA depende solo del color: cada forma lleva
 * icono propio (billete / tarjeta) y texto. El total (con IVA) sigue siendo lo
 * que cuesta; entregado y cambio son solo informativos.
 */

fun etiquetaFormaPago(forma: FormaPago): String = when (forma) {
    FormaPago.EFECTIVO -> "Efectivo"
    FormaPago.TARJETA -> "Tarjeta"
    FormaPago.NO_INDICADA -> "No indicada"
}

/** Frase del papel: "Pagado en efectivo" / "Pagado con tarjeta"; `null` si no se indica. */
fun textoPagadoCon(forma: FormaPago): String? = when (forma) {
    FormaPago.EFECTIVO -> "Pagado en efectivo"
    FormaPago.TARJETA -> "Pagado con tarjeta"
    FormaPago.NO_INDICADA -> null
}

fun iconoFormaPago(forma: FormaPago): ImageVector = when (forma) {
    FormaPago.EFECTIVO -> Icons.Outlined.Payments
    FormaPago.TARJETA -> Icons.Outlined.CreditCard
    FormaPago.NO_INDICADA -> Icons.Outlined.RemoveCircleOutline
}

/**
 * Fondo del chip/opcion activa (tokens existentes): efectivo en menta claro
 * (`tertiaryFixed`, texto `onTertiaryFixed`) y tarjeta en verde bosque oscuro
 * (`primary`, texto `onPrimary`): luminosidad opuesta + icono distinto, ambos
 * con contraste AA; la diferencia nunca depende solo del color.
 */
@Composable
fun contenedorFormaPago(forma: FormaPago): Color = when (forma) {
    FormaPago.EFECTIVO -> MaterialTheme.colorScheme.tertiaryFixed
    FormaPago.TARJETA -> MaterialTheme.colorScheme.primary
    FormaPago.NO_INDICADA -> MaterialTheme.colorScheme.surfaceContainerHigh
}

@Composable
fun contenidoFormaPago(forma: FormaPago): Color = when (forma) {
    FormaPago.EFECTIVO -> MaterialTheme.colorScheme.onTertiaryFixed
    FormaPago.TARJETA -> MaterialTheme.colorScheme.onPrimary
    FormaPago.NO_INDICADA -> MaterialTheme.colorScheme.onSurfaceVariant
}

/**
 * Chip pequeno con icono + texto de la forma de pago. NO_INDICADA no pinta nada
 * (ni chip ni hueco). Una sola parada de TalkBack: "Forma de pago: Efectivo".
 */
@Composable
fun ChipFormaPago(forma: FormaPago, modifier: Modifier = Modifier) {
    if (forma == FormaPago.NO_INDICADA) return
    val contenido = contenidoFormaPago(forma)
    Row(
        modifier = modifier
            .semantics { contentDescription = "Forma de pago: ${etiquetaFormaPago(forma)}" }
            .background(contenedorFormaPago(forma), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = iconoFormaPago(forma),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = contenido,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = etiquetaFormaPago(forma),
            style = MaterialTheme.typography.labelSmall,
            color = contenido,
            maxLines = 1,
        )
    }
}

/**
 * Bloque de pago del papel de solo lectura (resultado del procesado): filas
 * "Entregado" y "Cambio" bajo el total (solo efectivo, solo si hay dato) y la
 * linea "Pagado en efectivo" / "Pagado con tarjeta". NO_INDICADA no pinta nada.
 * Entregado y cambio son informativos: no suman en ninguna pantalla.
 *
 * Tags: `papel_forma_pago`, `papel_entregado`, `papel_cambio`.
 */
@Composable
fun PagoEnPapel(
    formaPago: FormaPago,
    entregado: Double?,
    cambio: Double?,
    modifier: Modifier = Modifier,
) {
    val frase = textoPagadoCon(formaPago) ?: return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (formaPago == FormaPago.EFECTIVO) {
            if (entregado != null) FilaPagoPapel("Entregado", entregado, "papel_entregado")
            if (cambio != null) FilaPagoPapel("Cambio", cambio, "papel_cambio")
        }
        Row(
            modifier = Modifier
                .padding(top = 4.dp)
                .semantics(mergeDescendants = true) {}
                .testTag("papel_forma_pago"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = iconoFormaPago(formaPago),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = frase,
                style = reciboTexto(MaterialTheme.typography.labelLarge)
                    .copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilaPagoPapel(etiqueta: String, importe: Double, tag: String) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .testTag(tag),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = etiqueta,
            style = reciboTexto(MaterialTheme.typography.bodyMedium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatoImporte(importe),
            style = reciboTexto(MaterialTheme.typography.bodyMedium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
