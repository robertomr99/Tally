package com.robertomr.tally.feature.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.robertomr.tally.domain.expenses.entities.Ticket
import java.time.LocalDate

/** Radio de la tarjeta de cada día (mockup 03: `rounded-xl`). */
private val RADIO_TARJETA = 12.dp

/** Texto mostrado cuando el comercio extraído está en blanco. */
const val TEXTO_SIN_COMERCIO = "Sin comercio"

/**
 * Forma de la fila según su posición en el grupo del día, para que las filas
 * sueltas de una `LazyColumn` compongan una única tarjeta redondeada.
 */
fun formaFilaTicket(esPrimera: Boolean, esUltima: Boolean): Shape = RoundedCornerShape(
    topStart = if (esPrimera) RADIO_TARJETA else 0.dp,
    topEnd = if (esPrimera) RADIO_TARJETA else 0.dp,
    bottomStart = if (esUltima) RADIO_TARJETA else 0.dp,
    bottomEnd = if (esUltima) RADIO_TARJETA else 0.dp,
)

/**
 * Cabecera de un día del listado (mockup 03): fecha a la izquierda y
 * `Total: X €` a la derecha. Los tags son `<tag>` y `<tag>_total`.
 */
@Composable
fun CabeceraDia(
    fecha: LocalDate,
    hoy: LocalDate,
    totalCentimos: Long,
    tag: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = etiquetaDia(fecha, hoy),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = "Total: ${formatoImporte(totalCentimos / 100.0)}",
            modifier = Modifier.testTag("${tag}_total"),
            style = MaterialTheme.typography.labelLarge.merge(EstiloTabular),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Fila de ticket del listado (mockup 03): icono de categoría en círculo de 40 dp,
 * comercio (1 línea), hora + chip con el nombre de la categoría y total a la
 * derecha. Toda la fila es clicable (>= 48 dp). Con [mostrarFechaCorta] se
 * antepone la fecha corta (respecto a [hoy]) (vista previa de Resumen, sin cabecera de día).
 *
 * Tags: `<tag>` en la fila y `<tag>_comercio|_total|_categoria|_forma_pago` en
 * los hijos. El importe es siempre `ticket.total` (nunca entregado ni cambio).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilaTicket(
    ticket: Ticket,
    tag: String,
    onClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    forma: Shape = RoundedCornerShape(RADIO_TARJETA),
    conSeparador: Boolean = false,
    mostrarFechaCorta: Boolean = false,
    hoy: LocalDate = LocalDate.now(),
) {
    val comercio = ticket.comercio.ifBlank { TEXTO_SIN_COMERCIO }
    val color = colorCategoria(ticket.categoria)
    val hora = textoHora(ticket.fechaEmision)
    val fecha = if (mostrarFechaCorta) etiquetaFechaCorta(ticket.fechaEmision.toLocalDate(), hoy) else null
    val momento = listOfNotNull(fecha, hora).joinToString(" · ")

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = forma,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column {
            if (conSeparador) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable(onClickLabel = "Abrir ticket de $comercio") { onClick(ticket.id) }
                    .testTag(tag)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = iconoCategoria(ticket.categoria),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = color,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = comercio,
                        modifier = Modifier.testTag("${tag}_comercio"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.size(2.dp))
                    // FlowRow: con la forma de pago el tercer chip baja de linea en 360 dp
                    // o con fuente grande en vez de cortarse (spec forma-pago).
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (momento.isNotEmpty()) {
                            Text(
                                text = momento,
                                style = MaterialTheme.typography.labelMedium.merge(EstiloTabular),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        ChipCategoria(ticket, color, Modifier.testTag("${tag}_categoria"))
                        // Efectivo/tarjeta con icono + texto; NO_INDICADA no pinta chip.
                        ChipFormaPago(ticket.formaPago, Modifier.testTag("${tag}_forma_pago"))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = formatoImporte(ticket.total),
                    modifier = Modifier.testTag("${tag}_total"),
                    style = MaterialTheme.typography.titleMedium.merge(EstiloTabular),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * Chip con el nombre de la categoría: fondo tintado y texto en `onSurfaceVariant`
 * (el color de categoría no alcanza 4,5:1 como texto; la identidad por color va en
 * el icono y el nombre evita depender solo del color).
 */
@Composable
private fun ChipCategoria(ticket: Ticket, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Text(
        text = etiquetaCategoria(ticket.categoria),
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
