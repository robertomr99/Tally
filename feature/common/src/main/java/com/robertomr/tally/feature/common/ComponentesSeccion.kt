package com.robertomr.tally.feature.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Tarjeta de sección: el único lenguaje de tarjeta de la app (Resumen y Ajustes).
 * Fondo `surfaceContainerLow`, borde `outlineVariant` al 40 %, radio 24 dp y
 * padding interior de 20 dp. El [modifier] (ancho, tag) lo pone el llamador.
 */
@Composable
fun TarjetaSeccion(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = verticalArrangement,
            content = content,
        )
    }
}

/**
 * Título de sección (`titleMedium` SemiBold) marcado como `heading` para TalkBack.
 * Es el mismo estilo en Resumen, Ajustes y el formulario del ticket.
 */
@Composable
fun TituloSeccion(
    texto: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = texto,
        modifier = modifier.semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

/**
 * Estado de pantalla (vacío, error, permiso): icono de 40 dp opcional, titular
 * `headlineSmall`, mensaje opcional y un hueco [contenido] para botones o notas.
 * Centrado en vertical con 24 dp laterales. El tamaño y el tag los decide el
 * llamador mediante [modifier] (normalmente `fillMaxSize()` o `weight(1f)`).
 */
@Composable
fun EstadoPantalla(
    titulo: String,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    colorIcono: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    mensaje: String? = null,
    colorMensaje: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    mensajeModifier: Modifier = Modifier,
    contenido: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        if (icono != null) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = colorIcono,
            )
        }
        Text(
            text = titulo,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (mensaje != null) {
            Text(
                text = mensaje,
                modifier = mensajeModifier,
                style = MaterialTheme.typography.bodyMedium,
                color = colorMensaje,
                textAlign = TextAlign.Center,
            )
        }
        contenido()
    }
}

/** Carga de pantalla: indicador circular de 40 dp centrado. El tag lo pasa el llamador en [indicadorModifier]. */
@Composable
fun CargandoPantalla(
    modifier: Modifier = Modifier,
    indicadorModifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = indicadorModifier.size(40.dp))
    }
}
