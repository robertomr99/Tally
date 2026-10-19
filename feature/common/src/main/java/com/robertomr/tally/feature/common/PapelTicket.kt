package com.robertomr.tally.feature.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt

/*
 * Papel de ticket (spec papel-ticket): envoltorio visual compartido por el
 * resultado del procesado, la revision y el detalle. Solo cambia la forma de
 * presentar el ticket digitalizado; los datos y la edicion no se tocan.
 *
 * Excepciones deliberadas a DESIGN.md (documentadas alli): tipografia
 * monoespaciada del sistema SOLO dentro del papel, sombra de 2 dp del papel y
 * papel siempre claro (la app no tiene tema oscuro).
 */

/** Alto del diente del serrado; el contenido del papel se separa de los bordes por este valor mas un margen. */
private val DIENTE_ALTO = 6.dp
private val DIENTE_ANCHO = 12.dp

/** Ancho maximo del papel: en tablet se centra en vez de estirarse. */
private val PAPEL_ANCHO_MAXIMO = 480.dp

/**
 * Hoja de papel de ticket: fondo blanco (`surfaceContainerLowest`), bordes
 * superior e inferior serrados dibujados con un [Shape], sombra sutil y
 * hairline. No anade semantica: el serrado es decorativo. El contenido se
 * coloca en una columna con padding interior que lo mantiene fuera del serrado.
 */
@Composable
fun PapelTicket(
    modifier: Modifier = Modifier,
    testTag: String = "papel_ticket",
    content: @Composable ColumnScope.() -> Unit,
) {
    val forma = remember { TicketSerradoShape(DIENTE_ANCHO, DIENTE_ALTO) }
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = PAPEL_ANCHO_MAXIMO)
                .fillMaxWidth()
                .shadow(elevation = 2.dp, shape = forma)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest, forma)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    shape = forma,
                )
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = DIENTE_ALTO + 20.dp,
                    bottom = DIENTE_ALTO + 20.dp,
                )
                .testTag(testTag),
            content = content,
        )
    }
}

/**
 * Forma de hoja con dientes triangulares arriba y abajo. El numero de dientes
 * se ajusta al ancho ([numeroDeDientes]) para que el serrado termine rasante en
 * ambos extremos a cualquier ancho.
 */
class TicketSerradoShape(
    private val dienteAncho: Dp = 12.dp,
    private val dienteAlto: Dp = 6.dp,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val alto = with(density) { dienteAlto.toPx() }
        val n = numeroDeDientes(size.width, with(density) { dienteAncho.toPx() })
        val ancho = size.width / n
        val camino = Path().apply {
            moveTo(0f, alto)
            for (i in 0 until n) {
                lineTo(i * ancho + ancho / 2f, 0f)
                lineTo((i + 1) * ancho, alto)
            }
            lineTo(size.width, size.height - alto)
            for (i in n - 1 downTo 0) {
                lineTo(i * ancho + ancho / 2f, size.height)
                lineTo(i * ancho, size.height - alto)
            }
            close()
        }
        return Outline.Generic(camino)
    }
}

/** Numero de dientes que caben en [ancho] con dientes de ~[dienteAncho]; al menos 1. */
internal fun numeroDeDientes(ancho: Float, dienteAncho: Float): Int =
    max(1, (ancho / dienteAncho).roundToInt())

/** Importe de una fila de recibo: cantidad por precio unitario (misma formula que `totalRecalculado`). */
fun importeDeLinea(cantidad: Int, precioUnitario: Double): Double = cantidad * precioUnitario

/** Estilo de recibo: monoespaciada del sistema con cifras tabulares. Solo dentro del papel. */
fun reciboTexto(base: TextStyle): TextStyle =
    base.copy(fontFamily = FontFamily.Monospace, fontFeatureSettings = "tnum")

/** Separador punteado de recibo (puntos redondos); decorativo, sin semantica. */
@Composable
fun SeparadorPunteado(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.outline
    Canvas(modifier = modifier.fillMaxWidth().height(4.dp)) {
        val grosor = 1.5.dp.toPx()
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = grosor,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.01f, grosor * 2.6f)),
        )
    }
}

/**
 * Fila de recibo de solo lectura: `n×`, concepto (varias lineas), guia de
 * puntos hasta el importe y el importe a la derecha. Si el concepto es largo la
 * guia se omite en la ultima linea cuando no queda hueco.
 *
 * @param importe importe ya formateado de la fila (cantidad por precio).
 * @param detalle texto secundario bajo la fila (p. ej. `3 × 1,99 €`), opcional.
 */
@Composable
fun FilaRecibo(
    cantidad: Int,
    concepto: String,
    importe: String,
    modifier: Modifier = Modifier,
    detalle: String? = null,
) {
    val estilo = reciboTexto(MaterialTheme.typography.bodyMedium)
    val estiloImporte = estilo.copy(fontWeight = FontWeight.SemiBold)
    val colorTexto = MaterialTheme.colorScheme.onSurface
    val colorGuia = MaterialTheme.colorScheme.outline
    // Con fuentes muy grandes el concepto se quedaria en una columna de pocas
    // letras: el importe baja a su propia linea, a la derecha.
    val apilar = LocalDensity.current.fontScale > 1.4f
    var capa by remember { mutableStateOf<TextLayoutResult?>(null) }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "$cantidad×",
                style = estilo,
                color = colorTexto,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .drawBehind {
                        val c = capa ?: return@drawBehind
                        if (apilar) return@drawBehind
                        val ultima = c.lineCount - 1
                        val x0 = c.getLineRight(ultima) + 6.dp.toPx()
                        val x1 = size.width
                        if (x1 - x0 >= 16.dp.toPx()) {
                            val y = c.getLineBaseline(ultima) - 1.dp.toPx()
                            drawLine(
                                color = colorGuia,
                                start = Offset(x0, y),
                                end = Offset(x1, y),
                                strokeWidth = 1.dp.toPx(),
                                cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(0.01f, 4.dp.toPx()),
                                ),
                            )
                        }
                    },
            ) {
                Text(
                    text = concepto,
                    style = estilo,
                    color = colorTexto,
                    onTextLayout = { capa = it },
                )
            }
            if (!apilar) {
                Text(
                    text = importe,
                    style = estiloImporte,
                    color = colorTexto,
                    modifier = Modifier.align(Alignment.Bottom),
                )
            }
        }
        if (apilar) {
            Text(
                text = importe,
                style = estiloImporte,
                color = colorTexto,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (detalle != null) {
            Text(
                text = detalle,
                style = reciboTexto(MaterialTheme.typography.labelSmall),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 28.dp),
            )
        }
    }
}
