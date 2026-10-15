package com.robertomr.tally.feature.escanear

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pantalla 05: revision de la foto ya procesada (mockup 05, spec captura-foto).
 * La foto se decodifica con BitmapFactory (sin libreria de imagenes); el dock
 * lleva "Repetir foto" (borra la foto del borrador reemplazado) y "Continuar"
 * DESHABILITADO hasta que `procesado-ui` lo cablee (non-goal de esta feature).
 *
 * Decoracion ficticia del mockup NO implementada (DESIGN.md R4): chip "Foto
 * nitida / Recorte automatico", boton de esquinas (crop), zoom/rotar, badge de
 * conceptos identificados.
 */
@Composable
fun RevisionFotoContent(
    rutaFoto: String,
    onRepetirFoto: () -> Unit,
    onContinuar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .testTag("captura_revision"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FotoGuardada(
            rutaFoto = rutaFoto,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        )

        DockAcciones(
            onRepetirFoto = onRepetirFoto,
            onContinuar = onContinuar,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        )
    }
}

@Composable
private fun FotoGuardada(
    rutaFoto: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val foto by produceState<ImageBitmap?>(initialValue = null, rutaFoto) {
        value = withContext(Dispatchers.IO) {
            BitmapFactory.decodeFile(File(context.filesDir, rutaFoto).absolutePath)?.asImageBitmap()
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val bitmap = foto
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Foto capturada del ticket",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .testTag("captura_foto"),
            )
        } else {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
        }
    }
}

/** Dock inferior del mockup 05: Repetir foto (secundario) + Continuar (primary). */
@Composable
private fun DockAcciones(
    onRepetirFoto: () -> Unit,
    onContinuar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = onRepetirFoto,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("captura_repetir"),
            shape = RoundedCornerShape(9999.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Replay,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Repetir foto")
        }

        // Deshabilitado hasta procesado-ui (non-goal de captura-foto).
        Button(
            onClick = onContinuar,
            enabled = false,
            modifier = Modifier
                .weight(1.4f)
                .height(48.dp)
                .testTag("captura_continuar"),
            shape = RoundedCornerShape(9999.dp),
            colors = ButtonDefaults.buttonColors(),
        ) {
            Text(text = "Continuar")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
