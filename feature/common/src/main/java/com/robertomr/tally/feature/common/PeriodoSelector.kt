package com.robertomr.tally.feature.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.robertomr.tally.domain.expenses.entities.PeriodoTipo

/**
 * Segmented control Día / Semana / Mes (mockup 02): píldora en
 * `surface-container-low`, segmento activo en `secondary-container` con check.
 */
@Composable
fun PeriodoSelector(
    seleccionado: PeriodoTipo,
    onSeleccionar: (PeriodoTipo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Segmento(PeriodoTipo.DIA, "Día", "periodo_dia", seleccionado, onSeleccionar, Modifier.weight(1f))
        Segmento(PeriodoTipo.SEMANA, "Semana", "periodo_semana", seleccionado, onSeleccionar, Modifier.weight(1f))
        Segmento(PeriodoTipo.MES, "Mes", "periodo_mes", seleccionado, onSeleccionar, Modifier.weight(1f))
    }
}

@Composable
private fun Segmento(
    tipo: PeriodoTipo,
    texto: String,
    tag: String,
    seleccionado: PeriodoTipo,
    onSeleccionar: (PeriodoTipo) -> Unit,
    modifier: Modifier,
) {
    val activo = tipo == seleccionado
    val fondo = if (activo) MaterialTheme.colorScheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent
    val contenido = if (activo) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(RoundedCornerShape(50))
            .background(fondo)
            .clickable(role = Role.Tab) { onSeleccionar(tipo) }
            .semantics { selected = activo }
            .testTag(tag)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (activo) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = contenido,
                modifier = Modifier.size(14.dp),
            )
            androidx.compose.foundation.layout.Spacer(Modifier.size(4.dp))
        }
        Text(
            text = texto,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (activo) FontWeight.SemiBold else FontWeight.Medium,
            color = contenido,
        )
    }
}
