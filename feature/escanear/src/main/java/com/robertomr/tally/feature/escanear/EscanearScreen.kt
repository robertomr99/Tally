package com.robertomr.tally.feature.escanear

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Pantalla Escanear (spec captura-foto): sustituye al placeholder de
 * navegacion-base por el subflujo de captura real.
 *
 * - La raiz conserva el testTag `pantalla_escanear` (regresion de
 *   NavigationTest) en TODOS los estados.
 * - Gate de permiso de camara in-app: sin dialogo automatico del sistema al
 *   entrar; boton "Permitir camara" que lanza el launcher. Si se deniega, el
 *   estado queda en espanol con boton para volver a pedirlo (sin bucles).
 * - Subflujo interno Encuadre <-> Revision (BackHandler) SIN rutas nuevas en
 *   :app: la pantalla 05 es un estado interno de esta feature.
 */
@Composable
fun EscanearScreen() {
    val viewModel: EscanearViewModel = hiltViewModel()
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val uiState = rememberEscanearState()
    val context = LocalContext.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { concedido ->
        viewModel.onPermisoCambiado(concedido)
        if (!concedido) uiState.onPermisoDenegado()
    }

    // El estado del permiso se informa al entrar (sin lanzar dialogos).
    LaunchedEffect(Unit) {
        val concedido = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.onPermisoCambiado(concedido)
    }

    // Back en Revision vuelve al encuadre descartando la foto (borrado
    // best-effort, misma semantica que "Repetir foto").
    BackHandler(enabled = estado is CapturaUiState.Revision) {
        viewModel.onRepetirFoto()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .testTag("pantalla_escanear"),
    ) {
        when (val actual = estado) {
            CapturaUiState.PermisoPendiente -> PermisoCamaraContent(
                permisoDenegadoAntes = uiState.permisoDenegadoAntes,
                onPermitir = { launcher.launch(Manifest.permission.CAMERA) },
            )

            CapturaUiState.Encuadre -> CameraEncuadre(
                onCapturado = viewModel::onImageCapturado,
                onCapturaFallida = viewModel::onCapturaFallida,
                onCerrar = { backDispatcher?.onBackPressed() },
            )

            CapturaUiState.Procesando -> ProcesandoContent()

            is CapturaUiState.Revision -> RevisionFotoContent(
                rutaFoto = actual.rutaFoto,
                textoCrudo = actual.textoCrudo,
                ocrNoDisponible = actual.ocrNoDisponible,
                onRepetirFoto = viewModel::onRepetirFoto,
                onContinuar = { /* cableado con procesado-ui (non-goal aqui) */ },
            )

            is CapturaUiState.Error -> ErrorContent(
                mensaje = actual.mensaje,
                onVolverAlEncuadre = viewModel::onRepetirFoto,
            )
        }
    }
}

/** Estado in-app de permiso denegado (escenario 6 del spec), copy en espanol. */
@Composable
private fun PermisoCamaraContent(
    permisoDenegadoAntes: Boolean,
    onPermitir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("captura_permiso"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.PhotoCamera,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Necesitamos la cámara",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tally usa la cámara para capturar el ticket de papel y guardar su foto.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (permisoDenegadoAntes) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Permiso denegado. Puedes activarlo en Ajustes > Aplicaciones > Tally > Permisos > Cámara.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onPermitir,
            modifier = Modifier
                .height(48.dp)
                .testTag("captura_permitir"),
            shape = RoundedCornerShape(9999.dp),
        ) {
            Text(text = "Permitir cámara")
        }
    }
}

@Composable
private fun ProcesandoContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("captura_procesando"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Procesando la foto\u2026",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Error de captura/procesado con vuelta al encuadre (sin crash). */
@Composable
private fun ErrorContent(
    mensaje: String,
    onVolverAlEncuadre: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("captura_error"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No se pudo guardar la foto",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = mensaje,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onVolverAlEncuadre,
            modifier = Modifier
                .height(48.dp)
                .testTag("captura_reintentar"),
            shape = RoundedCornerShape(9999.dp),
        ) {
            Text(text = "Volver al encuadre")
        }
    }
}
