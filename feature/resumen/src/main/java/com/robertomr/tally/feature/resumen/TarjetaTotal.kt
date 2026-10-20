package com.robertomr.tally.feature.resumen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.robertomr.tally.domain.expenses.entities.ResumenPeriodo
import com.robertomr.tally.feature.common.EstiloTabular
import com.robertomr.tally.feature.common.TarjetaSeccion
import com.robertomr.tally.feature.common.formatoImporte

/**
 * Tarjeta de total del mockup 02: etiqueta + rango, total en display con cifras
 * tabulares, badge comparativo y dos micro-métricas. Sin ViewModel: recibe el
 * estado ya calculado. Mientras [resumen] es `null` (cargando) pinta "—".
 */
@Composable
internal fun TarjetaTotal(
    resumen: ResumenPeriodo?,
    modifier: Modifier = Modifier,
) {
    TarjetaSeccion(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // El rango del periodo ya está en el subtítulo de la cabecera (una sola vez).
        Text(
            text = "GASTO TOTAL DEL PERIODO",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = resumen?.let { formatoImporte(it.total) } ?: "—",
            modifier = Modifier.testTag("resumen_suma"),
            style = MaterialTheme.typography.displaySmall.merge(EstiloTabular),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (resumen != null) BadgeComparativa(resumen.variacion)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MicroMetrica(
                etiqueta = "Promedio diario",
                valor = resumen?.let { formatoPromedio(it.promedioDiario) } ?: "—",
                tag = "resumen_promedio",
                modifier = Modifier.weight(1f),
            )
            MicroMetrica(
                etiqueta = "Tickets",
                valor = resumen?.let { textoTickets(it.numTickets) } ?: "—",
                tag = "resumen_num_tickets",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun BadgeComparativa(variacion: Double?) {
    // Sin semántica verde/rojo (D4): gastar menos no es "bueno" ni "malo" para la app.
    val icono = when {
        variacion == null -> null
        variacion > 0.0 -> Icons.AutoMirrored.Filled.TrendingUp
        variacion < 0.0 -> Icons.AutoMirrored.Filled.TrendingDown
        else -> Icons.AutoMirrored.Filled.TrendingFlat
    }
    val contenedor = if (variacion == null) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
    }
    val colorTexto = if (variacion == null) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    val texto = if (variacion == null) TEXTO_SIN_REFERENCIA else "${formatoVariacion(variacion)} vs periodo anterior"
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(contenedor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icono != null) {
            Icon(imageVector = icono, contentDescription = null, modifier = Modifier.size(16.dp), tint = colorTexto)
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = texto,
            modifier = Modifier.testTag("resumen_comparativa"),
            style = MaterialTheme.typography.labelLarge.merge(EstiloTabular),
            color = colorTexto,
        )
    }
}

@Composable
private fun MicroMetrica(etiqueta: String, valor: String, tag: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(12.dp),
    ) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = valor,
            modifier = Modifier.testTag(tag),
            style = MaterialTheme.typography.titleSmall.merge(EstiloTabular),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
