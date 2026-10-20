package com.robertomr.tally.feature.escanear

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.robertomr.tally.feature.common.CabeceraPantalla
import com.robertomr.tally.feature.common.ColumnaCentrada
import com.robertomr.tally.feature.common.FotoOriginal
import com.robertomr.tally.feature.common.PADDING_PANTALLA
import com.robertomr.tally.feature.common.UMBRAL_APILAR_BOTONES

/**
 * Pantalla 05: revision de la foto ya procesada (mockup 05, spec captura-foto).
 * La foto se decodifica con BitmapFactory (sin libreria de imagenes); el dock
 * lleva "Repetir foto" (borra la foto del borrador reemplazado) y "Continuar",
 * que abre el procesado de la extraccion (spec procesado-ui, mockup 06) con el
 * Borrador vivo (foto + texto crudo).
 *
 * Panel de debug del texto crudo (spec ocr-mlkit): SOLO en builds debug
 * (`ApplicationInfo.FLAG_DEBUGGABLE`), testTag `captura_texto_crudo`. Es una
 * superficie de diagnostico, NO diseno (sin decoracion ficticia del mockup,
 * DESIGN.md R4).
 */
@Composable
fun RevisionFotoContent(
    rutaFoto: String,
    textoCrudo: String,
    ocrNoDisponible: Boolean,
    onRepetirFoto: () -> Unit,
    onContinuar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            // Mismo fondo que el resto de pantallas: la barra de estado (innerPadding del shell)
            // es `surface` y otro color aquí dibujaría una banda visible bajo el reloj.
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .testTag("captura_revision"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Título y controles en la columna única; la foto ocupa el ancho disponible.
        ColumnaCentrada { CabeceraPantalla(titulo = "Revisar foto") }

        FotoOriginal(
            rutaFoto = rutaFoto,
            testTagImagen = "captura_foto",
            testTagAusente = "captura_foto_ausente",
            contentDescription = "Foto capturada del ticket",
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = PADDING_PANTALLA, end = PADDING_PANTALLA, top = 16.dp, bottom = 8.dp),
        )

        ColumnaCentrada {
            Column {
                PanelDebugTextoCrudo(
                    textoCrudo = textoCrudo,
                    ocrNoDisponible = ocrNoDisponible,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = PADDING_PANTALLA, end = PADDING_PANTALLA),
                )

                DockAcciones(
                    onRepetirFoto = onRepetirFoto,
                    onContinuar = onContinuar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = PADDING_PANTALLA, end = PADDING_PANTALLA, top = 8.dp, bottom = 16.dp),
                )
            }
        }
    }
}

/**
 * Panel SOLO debug del texto crudo del OCR (spec ocr-mlkit): muestra el texto
 * registrado con el Borrador, "(OCR vacio)" si el motor no detecto texto o
 * "OCR no disponible" si el motor fallo (la captura no se bloquea nunca). En
 * builds release no se compone nada (el texto viaja igual con el Borrador).
 */
@Composable
private fun PanelDebugTextoCrudo(
    textoCrudo: String,
    ocrNoDisponible: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val esDebug = remember(context) {
        context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }
    if (!esDebug) return

    Column(modifier = modifier.testTag("captura_texto_crudo")) {
        Text(
            text = "Texto crudo (debug)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = when {
                ocrNoDisponible -> "OCR no disponible"
                textoCrudo.isBlank() -> "(OCR vac\u00edo)"
                else -> textoCrudo
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .heightIn(max = 96.dp)
                .verticalScroll(rememberScrollState()),
        )
    }
}

/** Dock inferior del mockup 05: Repetir foto (secundario) + Continuar (primary). */
@Composable
private fun DockAcciones(
    onRepetirFoto: () -> Unit,
    onContinuar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Con fuente grande "Repetir foto" se cortaba ("Rep"): los botones se apilan, como el
    // dock de edición (mismo umbral), con "Continuar" primero.
    val apilar = LocalDensity.current.fontScale > UMBRAL_APILAR_BOTONES
    val botonRepetir: @Composable (Modifier) -> Unit = { m ->
        OutlinedButton(
            onClick = onRepetirFoto,
            modifier = m
                .heightIn(min = 48.dp)
                .testTag("captura_repetir"),
            shape = RoundedCornerShape(9999.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Replay,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Repetir foto", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    // "Continuar" abre el procesado de la extraccion (spec procesado-ui).
    val botonContinuar: @Composable (Modifier) -> Unit = { m ->
        Button(
            onClick = onContinuar,
            enabled = true,
            modifier = m
                .heightIn(min = 48.dp)
                .testTag("captura_continuar"),
            shape = RoundedCornerShape(9999.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            colors = ButtonDefaults.buttonColors(),
        ) {
            Text(text = "Continuar", maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
    }

    if (apilar) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            botonContinuar(Modifier.fillMaxWidth())
            botonRepetir(Modifier.fillMaxWidth())
        }
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            botonRepetir(Modifier.weight(1f))
            botonContinuar(Modifier.weight(1f))
        }
    }
}
