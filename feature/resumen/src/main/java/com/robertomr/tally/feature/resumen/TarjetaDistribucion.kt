package com.robertomr.tally.feature.resumen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.robertomr.tally.domain.expenses.entities.DistribucionCategorias
import com.robertomr.tally.domain.expenses.entities.PorcionCategoria
import com.robertomr.tally.feature.common.colorCategoria
import com.robertomr.tally.feature.common.etiquetaCategoria
import com.robertomr.tally.feature.common.iconoCategoria

/** Ancho a partir del cual donut y leyenda van en fila (mockup: `flex-col sm:flex-row`). */
private val ANCHO_FILA = 480.dp

/** Texto equivalente del donut para accesibilidad: `Alimentación 52 %, Hogar 27 %`. */
internal fun descripcionDonut(distribucion: DistribucionCategorias?): String = when {
    distribucion == null -> "Distribución por categorías, cargando"
    distribucion.estaVacia -> "Distribución por categorías: sin gastos en este periodo"
    else -> "Distribución por categorías: " + distribucion.porciones.joinToString(", ") {
        "${etiquetaCategoria(it.categoria)} ${textoPorcentaje(it.porcentaje)}"
    }
}

/**
 * Tarjeta "Distribución por categorías" del mockup 02. Sin ViewModel: recibe el
 * reparto ya calculado en dominio. `null` = cargando (anillo de fondo y "—");
 * `estaVacia` = sin gasto (anillo de fondo, "Sin gastos" y copy explicativo).
 */
@Composable
internal fun TarjetaDistribucion(
    distribucion: DistribucionCategorias?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column {
                Text(
                    text = "Distribución por categorías",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (distribucion != null && !distribucion.estaVacia) {
                    Text(
                        text = textoCategoriasConGasto(distribucion.porciones.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            val porciones = distribucion?.porciones.orEmpty()
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val enFila = maxWidth >= ANCHO_FILA
                val donut: @Composable () -> Unit = { DonutConCentro(distribucion) }
                if (enFila) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        donut()
                        if (porciones.isNotEmpty()) LeyendaCategorias(porciones, Modifier.weight(1f))
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        donut()
                        if (porciones.isNotEmpty()) LeyendaCategorias(porciones, Modifier.fillMaxWidth())
                    }
                }
            }

            if (distribucion != null && distribucion.estaVacia) {
                Text(
                    text = "Aún no hay gastos en este periodo.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("donut_vacio"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            } else if (porciones.isNotEmpty()) {
                DesgloseCategorias(porciones)
            }
        }
    }
}

@Composable
private fun DonutConCentro(distribucion: DistribucionCategorias?) {
    val porciones = distribucion?.porciones.orEmpty()
    Box(contentAlignment = Alignment.Center) {
        DonutCategorias(porciones = porciones, descripcion = descripcionDonut(distribucion))
        Column(
            modifier = Modifier.testTag("donut_centro"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val mayor = porciones.firstOrNull()
            when {
                mayor != null -> {
                    Text(
                        text = "MAYOR GASTO",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = textoPorcentaje(mayor.porcentaje),
                        style = MaterialTheme.typography.titleMedium.merge(EstiloTabular),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = etiquetaCategoria(mayor.categoria),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                distribucion == null -> Text(
                    text = "—",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> Text(
                    text = "Sin gastos",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Rejilla de 2 columnas: punto de color + `Nombre (38 %)`. */
@Composable
private fun LeyendaCategorias(porciones: List<PorcionCategoria>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        porciones.chunked(2).forEach { fila ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fila.forEach { porcion ->
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("leyenda_${porcion.categoria.name}"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(colorCategoria(porcion.categoria)),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${etiquetaCategoria(porcion.categoria)} (${textoPorcentaje(porcion.porcentaje)})",
                            style = MaterialTheme.typography.labelLarge.merge(EstiloTabular),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                if (fila.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DesgloseCategorias(porciones: List<PorcionCategoria>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "DESGLOSE DETALLADO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "IMPORTE / RATIO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        porciones.forEach { FilaDesglose(it) }
    }
}

@Composable
private fun FilaDesglose(porcion: PorcionCategoria) {
    val color = colorCategoria(porcion.categoria)
    val nombre = porcion.categoria.name
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(12.dp)
            .testTag("desglose_$nombre"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = iconoCategoria(porcion.categoria),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.White,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = etiquetaCategoria(porcion.categoria),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatoImporte(porcion.centimos / 100.0),
                    modifier = Modifier.testTag("desglose_${nombre}_importe"),
                    style = MaterialTheme.typography.titleSmall.merge(EstiloTabular),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = textoPorcentaje(porcion.porcentaje),
                    modifier = Modifier.testTag("desglose_${nombre}_porcentaje"),
                    style = MaterialTheme.typography.labelMedium.merge(EstiloTabular),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // Barra tonal de 6 dp: el relleno es proporcional a la fracción exacta.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(porcion.fraccion.toFloat().coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}
