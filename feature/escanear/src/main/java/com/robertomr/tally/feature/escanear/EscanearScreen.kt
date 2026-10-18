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
 * - Subflujo interno Encuadre <-> Revision <-> Procesado <-> RevisionDatos
 *   (BackHandler) SIN rutas nuevas en :app (spec procesado-ui/revision-datos):
 *   la pantalla 05, el procesado (mockup 06) y la revision editable (mockup 07)
 *   son estados internos de esta feature.
 *
 * Al persistir el ticket (spec revision-datos) la pantalla emite
 * [onGastoGuardado] (el :app navega a la pestaña Gastos; la pantalla NO recibe
 * el NavController) y resetea el subflujo a Encuadre (R-REV3).
 */
@Composable
fun EscanearScreen(onGastoGuardado: () -> Unit) {
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

    // Back durante el procesado = Cancelar (spec procesado-ui, escenario 2):
    // aborta la extraccion y vuelve a la pantalla 05 con el Borrador intacto.
    BackHandler(enabled = estado is CapturaUiState.ProcesandoExtraccion) {
        viewModel.onCancelarProcesado()
    }

    // Back desde el resultado o el error = "Volver a la foto" (Borrador
    // intacto; nada se persiste en esta feature).
    BackHandler(
        enabled = estado is CapturaUiState.ExtraccionLista ||
            estado is CapturaUiState.ExtraccionFallida,
    ) {
        viewModel.onVolverALaFoto()
    }

    // Back desde la revision editable (spec revision-datos, escenario 6): no
    // escribe nada en Room y vuelve al resultado del procesado con el Borrador
    // intacto.
    BackHandler(enabled = estado is CapturaUiState.RevisionDatos) {
        viewModel.onVolverAlResultado()
    }

    // Durante el guardado (breve) el back se ignora: no se cancela a medias.
    BackHandler(enabled = estado is CapturaUiState.Guardando) { }

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
                onContinuar = viewModel::onContinuar,
            )

            // Spec procesado-ui (mockup 06): procesado -> resultado/error como
            // subflujo interno, sin rutas nuevas en :app.
            is CapturaUiState.ProcesandoExtraccion -> ProcesandoExtraccionContent(
                motor = viewModel.motor,
                onCancelar = viewModel::onCancelarProcesado,
            )

            is CapturaUiState.ExtraccionLista -> ExtraccionListaContent(
                extraido = actual.extraido,
                onRevisar = viewModel::onRevisar,
            )

            is CapturaUiState.ExtraccionFallida -> ExtraccionFallidaContent(
                mensaje = actual.mensaje,
                motor = viewModel.motor,
                onReintentar = viewModel::onReintentarExtraccion,
                onVolverALaFoto = viewModel::onVolverALaFoto,
            )

            // Spec revision-datos (mockup 07) + edicion-lineas (mockup 08): revision
            // editable de la cabecera y de las lineas; "Descartar" = back (no escribe).
            is CapturaUiState.RevisionDatos -> RevisionDatosContent(
                extraido = actual.extraido,
                datos = actual.datos,
                errorGuardado = actual.errorGuardado,
                onComercioEditado = viewModel::onComercioEditado,
                onFechaEditada = viewModel::onFechaEditada,
                onTotalEditado = viewModel::onTotalEditado,
                onCategoriaElegida = viewModel::onCategoriaElegida,
                onLineaAnadida = viewModel::onLineaAnadida,
                onLineaConceptoEditado = viewModel::onLineaConceptoEditado,
                onLineaCantidadEditada = viewModel::onLineaCantidadEditada,
                onLineaPrecioEditado = viewModel::onLineaPrecioEditado,
                onLineaEliminada = viewModel::onLineaEliminada,
                onDescartar = viewModel::onVolverAlResultado,
                onGuardar = viewModel::onGuardar,
            )

            CapturaUiState.Guardando -> GuardandoContent()

            is CapturaUiState.Guardado -> {
                // El ticket ya esta persistido: se emite el evento de vuelta al
                // listado y se resetea el subflujo (R-REV3). La clave por id
                // evita relanzar el efecto.
                LaunchedEffect(actual.id) {
                    onGastoGuardado()
                    viewModel.onGuardadoVisto()
                }
                GuardandoContent()
            }
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

/** Guardado del ticket en curso (breve): persistiendo en Room. */
@Composable
private fun GuardandoContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("revision_guardando"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Guardando el ticket\u2026",
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
