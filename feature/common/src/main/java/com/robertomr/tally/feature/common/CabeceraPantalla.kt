package com.robertomr.tally.feature.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Cabecera común de pantalla: flecha atrás opcional, título `titleLarge` SemiBold,
 * subtítulo opcional `labelLarge` y una acción final opcional. Misma altura
 * mínima y mismo padding en todas las pantallas. No aplica insets: los pone el
 * llamador (barra de estado). Los tags los pasa el llamador ([backTag],
 * [subtituloTag]; la acción lleva el suyo dentro del lambda).
 */
@Composable
fun CabeceraPantalla(
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    subtituloTag: String? = null,
    onBack: (() -> Unit)? = null,
    backTag: String? = null,
    accion: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(
                start = if (onBack != null) 4.dp else 20.dp,
                end = when {
                    accion == null -> 20.dp
                    onBack != null -> 4.dp
                    else -> 8.dp
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .then(if (backTag != null) Modifier.testTag(backTag) else Modifier),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Volver",
                )
            }
        }
        Column(modifier = Modifier.weight(1f).padding(start = if (onBack != null) 4.dp else 0.dp)) {
            Text(
                text = titulo,
                // Título de pantalla para TalkBack (el TopAppBar de Ajustes ya lo hacía).
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                // Dos líneas: con fuente grande "Detalle del ticket" baja de línea en vez de cortarse (V22).
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitulo != null) {
                Text(
                    text = subtitulo,
                    modifier = if (subtituloTag != null) Modifier.testTag(subtituloTag) else Modifier,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        accion?.invoke(this)
    }
}
