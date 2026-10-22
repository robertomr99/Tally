package com.robertomr.tally.feature.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.robertomr.tally.domain.expenses.entities.FiltroFormaPago
import com.robertomr.tally.domain.expenses.entities.FormaPago

/**
 * Filtro de forma de pago Todos / Efectivo / Tarjeta (spec filtro-forma-pago):
 * tres `FilterChip` de selección única (semántica de RadioButton) de al menos
 * 48 dp. El activo lleva check y `secondaryContainer` (como [PeriodoSelector]);
 * cada opción, su icono. Un `FlowRow` evita recortes: con letra grande los chips
 * pasan a la fila siguiente en vez de truncarse.
 *
 * Tags: contenedor `filtro_pago`; chips `filtro_pago_todos`, `filtro_pago_efectivo`,
 * `filtro_pago_tarjeta`.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FiltroFormaPagoSelector(
    seleccionado: FiltroFormaPago,
    onSeleccionar: (FiltroFormaPago) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.testTag("filtro_pago"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (opcion in FiltroFormaPago.entries) {
            val activo = opcion == seleccionado
            FilterChip(
                selected = activo,
                onClick = { onSeleccionar(opcion) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics { role = Role.RadioButton }
                    .testTag(tagFiltro(opcion)),
                label = { Text(etiquetaFiltro(opcion), maxLines = 1) },
                leadingIcon = {
                    Icon(
                        imageVector = if (activo) Icons.Filled.Check else iconoFiltro(opcion),
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            )
        }
    }
}

private fun etiquetaFiltro(filtro: FiltroFormaPago): String = when (filtro) {
    FiltroFormaPago.TODOS -> "Todos"
    FiltroFormaPago.EFECTIVO -> etiquetaFormaPago(FormaPago.EFECTIVO)
    FiltroFormaPago.TARJETA -> etiquetaFormaPago(FormaPago.TARJETA)
}

private fun iconoFiltro(filtro: FiltroFormaPago): ImageVector = when (filtro) {
    FiltroFormaPago.TODOS -> Icons.Outlined.ReceiptLong
    FiltroFormaPago.EFECTIVO -> iconoFormaPago(FormaPago.EFECTIVO)
    FiltroFormaPago.TARJETA -> iconoFormaPago(FormaPago.TARJETA)
}

private fun tagFiltro(filtro: FiltroFormaPago): String = when (filtro) {
    FiltroFormaPago.TODOS -> "filtro_pago_todos"
    FiltroFormaPago.EFECTIVO -> "filtro_pago_efectivo"
    FiltroFormaPago.TARJETA -> "filtro_pago_tarjeta"
}
