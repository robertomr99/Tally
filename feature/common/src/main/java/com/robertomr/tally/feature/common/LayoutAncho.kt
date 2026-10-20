package com.robertomr.tally.feature.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Ancho total de la columna de contenido en tablet u horizontal (mockup 03:
 * `max-w-3xl`). Incluye los paddings horizontales de cada pantalla. Por debajo de
 * este ancho no tiene efecto: el layout es el de móvil.
 */
val ANCHO_MAXIMO_CONTENIDO = 720.dp

/**
 * Margen horizontal de pantalla: el mismo en cabecera, selector, tarjetas, listas,
 * formularios y docks. El papel de ticket conserva su padding interior de 16 dp.
 */
val PADDING_PANTALLA = 20.dp

/**
 * Escala de fuente a partir de la cual los pares de botones de un dock (Descartar/Guardar,
 * Repetir foto/Continuar) se apilan en vertical, con el botón principal primero.
 */
const val UMBRAL_APILAR_BOTONES = 1.15f

/**
 * Contenedor único de las pantallas: centra el contenido en una columna de
 * [ANCHO_MAXIMO_CONTENIDO]. El [modifier] se aplica al `Box` exterior (a todo el
 * ancho), de modo que fondos, scroll y `fillMaxSize` los decide el llamador fuera
 * de la columna. Dentro cabe `fillMaxHeight`/`matchParentSize` vía [BoxScope].
 */
@Composable
fun ColumnaCentrada(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Box(modifier = Modifier.widthIn(max = ANCHO_MAXIMO_CONTENIDO).fillMaxWidth(), content = content)
    }
}
