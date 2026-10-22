package com.robertomr.tally.feature.escanear

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.robertomr.tally.feature.common.ColumnaCentrada
import com.robertomr.tally.feature.common.EstadoPantallaDesplazable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

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

    // Spec galeria-escaneo: selector de fotos del sistema (Photo Picker, sin
    // permisos de almacenamiento). Cancelar (uri nulo) no cambia nada. La
    // lectura del Uri es acotada y va fuera del hilo principal; al VM solo
    // llegan los bytes (como con la camara) o el mensaje de error.
    val scope = rememberCoroutineScope()
    val selectorGaleria = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    viewModel.onImagenElegida(leerBytesAcotado(context.contentResolver, uri))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    viewModel.onImagenFallida(mensajeDeLectura(e))
                }
            }
        }
    }
    val elegirDeGaleria = {
        selectorGaleria.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
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
        // Cámara/encuadre, revisión de foto y revisión de datos van a todo el ancho (cada una
        // limita por dentro su cabecera, controles y dock); el resto, en la columna de 720 dp.
        val enColumna = estado !is CapturaUiState.Encuadre && estado !is CapturaUiState.Revision &&
            estado !is CapturaUiState.RevisionDatos
        ContenedorEscanear(enColumna = enColumna) {
            when (val actual = estado) {
                CapturaUiState.PermisoPendiente -> PermisoCamaraContent(
                    permisoDenegadoAntes = uiState.permisoDenegadoAntes,
                    onPermitir = { launcher.launch(Manifest.permission.CAMERA) },
                    onElegirGaleria = elegirDeGaleria,
                )

                CapturaUiState.Encuadre -> CameraEncuadre(
                    onCapturado = viewModel::onImageCapturado,
                    onCapturaFallida = viewModel::onCapturaFallida,
                    onCerrar = { backDispatcher?.onBackPressed() },
                    onElegirGaleria = elegirDeGaleria,
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
                    motor = actual.motor,
                    onCancelar = viewModel::onCancelarProcesado,
                )

                is CapturaUiState.ExtraccionLista -> ExtraccionListaContent(
                    extraido = actual.extraido,
                    otro = actual.otro,
                    onRevisar = viewModel::onRevisar,
                    onReintentarOtro = viewModel::onReintentarConOtroMotor,
                )

                is CapturaUiState.ExtraccionFallida -> ExtraccionFallidaContent(
                    mensaje = actual.mensaje,
                    motor = actual.motor,
                    otro = actual.otro,
                    hayAnterior = actual.anterior != null,
                    onReintentar = viewModel::onReintentarExtraccion,
                    onReintentarOtro = viewModel::onReintentarConOtroMotor,
                    onVolverALaFoto = viewModel::onVolverALaFoto,
                    onVolverAnterior = viewModel::onVolverAlResultadoAnterior,
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
                    onFormaPagoElegida = viewModel::onFormaPagoElegida,
                    onEntregadoEditado = viewModel::onEntregadoEditado,
                    onCambioEditado = viewModel::onCambioEditado,
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
}

/** Envuelve el estado en la columna centrada común salvo cámara y foto a todo el ancho. */
@Composable
private fun ContenedorEscanear(enColumna: Boolean, content: @Composable () -> Unit) {
    if (enColumna) {
        ColumnaCentrada(modifier = Modifier.fillMaxSize()) { content() }
    } else {
        content()
    }
}

/** Estado in-app de permiso denegado (escenario 6 del spec), copy en espanol. */
@Composable
private fun PermisoCamaraContent(
    permisoDenegadoAntes: Boolean,
    onPermitir: () -> Unit,
    onElegirGaleria: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EstadoPantallaDesplazable(
        titulo = "Necesitamos la cámara",
        modifier = modifier
            .fillMaxSize()
            .testTag("captura_permiso"),
        icono = Icons.Outlined.PhotoCamera,
        colorIcono = MaterialTheme.colorScheme.primary,
        mensaje = "Tally usa la cámara para capturar el ticket de papel y guardar su foto. " +
            "O elige una foto de tu galería.",
    ) {
        if (permisoDenegadoAntes) {
            Text(
                text = "Permiso denegado. Puedes activarlo en Ajustes > Aplicaciones > Tally > Permisos > Cámara.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Button(
            onClick = onPermitir,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("captura_permitir"),
            shape = RoundedCornerShape(9999.dp),
        ) {
            Text(text = "Permitir cámara")
        }
        // Alternativa sin permiso (spec galeria-escaneo): el alto es minimo (no
        // fijo) para que el texto envuelva con letra grande.
        OutlinedButton(
            onClick = onElegirGaleria,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("permiso_galeria"),
            shape = RoundedCornerShape(9999.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Elegir de la galería", textAlign = TextAlign.Center)
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
    EstadoPantallaDesplazable(
        titulo = "No se pudo guardar la foto",
        modifier = modifier
            .fillMaxSize()
            .testTag("captura_error"),
        icono = Icons.Outlined.ErrorOutline,
        colorIcono = MaterialTheme.colorScheme.error,
        mensaje = mensaje,
    ) {
        Button(
            onClick = onVolverAlEncuadre,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("captura_reintentar"),
            shape = RoundedCornerShape(9999.dp),
        ) {
            Text(text = "Volver al encuadre")
        }
    }
}
