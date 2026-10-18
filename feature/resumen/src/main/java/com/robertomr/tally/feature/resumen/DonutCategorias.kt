package com.robertomr.tally.feature.resumen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.robertomr.tally.domain.expenses.entities.PorcionCategoria
import com.robertomr.tally.feature.common.colorCategoria

/** Ángulo de arranque del donut: las 12 h, en sentido horario (Canvas mide desde las 3 h). */
internal const val ANGULO_INICIO_DONUT = -90f

/** Hueco entre arcos (grados) cuando hay más de una porción. */
internal const val HUECO_DONUT_GRADOS = 2f

/** Arco del donut en grados de Canvas: [inicio] y [barrido] (horario). */
internal data class SegmentoDonut(val inicio: Float, val barrido: Float)

/**
 * Ángulos de cada porción, proporcionales a `fraccion` (no al porcentaje
 * redondeado). Con una sola porción el anillo es completo (360°, sin hueco);
 * con varias, cada arco cede [huecoGrados] al hueco que lo sigue, con un
 * barrido mínimo de 1° para que una porción diminuta siga visible. Función
 * pura para poder probarla en JVM.
 */
internal fun segmentosDonut(
    porciones: List<PorcionCategoria>,
    huecoGrados: Float = HUECO_DONUT_GRADOS,
): List<SegmentoDonut> {
    if (porciones.isEmpty()) return emptyList()
    if (porciones.size == 1) return listOf(SegmentoDonut(ANGULO_INICIO_DONUT, 360f))
    var cursor = ANGULO_INICIO_DONUT
    return porciones.map { porcion ->
        val reservado = (porcion.fraccion * 360.0).toFloat()
        val segmento = SegmentoDonut(inicio = cursor, barrido = (reservado - huecoGrados).coerceAtLeast(1f))
        cursor += reservado
        segmento
    }
}

/**
 * Donut dibujado con `Canvas` (sin librerías de gráficas). Sin porciones pinta
 * solo la pista de fondo. La descripción de accesibilidad lleva el texto
 * equivalente; la información nunca depende solo del color.
 */
@Composable
internal fun DonutCategorias(
    porciones: List<PorcionCategoria>,
    descripcion: String,
    modifier: Modifier = Modifier,
    tamano: Dp = 176.dp,
) {
    val pista = MaterialTheme.colorScheme.surfaceContainerHighest
    val segmentos = segmentosDonut(porciones)
    Canvas(
        modifier = modifier
            .size(tamano)
            .testTag("donut_grafica")
            .semantics { contentDescription = descripcion },
    ) {
        // Grosor del mockup: trazo 12 sobre viewBox 100 (12 %).
        val grosor = size.minDimension * 0.12f
        val lado = size.minDimension - grosor
        val origen = Offset(grosor / 2f, grosor / 2f)
        val area = Size(lado, lado)
        drawArc(
            color = pista,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = origen,
            size = area,
            style = Stroke(width = grosor, cap = StrokeCap.Butt),
        )
        segmentos.forEachIndexed { i, s ->
            drawArc(
                color = colorCategoria(porciones[i].categoria),
                startAngle = s.inicio,
                sweepAngle = s.barrido,
                useCenter = false,
                topLeft = origen,
                size = area,
                style = Stroke(width = grosor, cap = StrokeCap.Butt),
            )
        }
    }
}
