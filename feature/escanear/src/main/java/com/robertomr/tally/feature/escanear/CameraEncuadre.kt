package com.robertomr.tally.feature.escanear

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.robertomr.tally.domain.expenses.data.CropRegion
import com.robertomr.tally.feature.common.ANCHO_MAXIMO_CONTENIDO
import java.io.ByteArrayOutputStream

/**
 * Encuadre guiado de la captura (mockup 04, spec captura-foto): preview de
 * camara en vivo con reticula normativa (scrim ~65% fuera de la zona de
 * captura + marcas de esquina + hint "Alinea los bordes del ticket dentro del
 * recuadro"), obturador circular 80dp, "Galería" y "Cerrar".
 *
 * Spec galeria-escaneo: "Galería" (a la izquierda del obturador) es funcional
 * y no depende de la cámara: lanza el selector del sistema mediante
 * [onElegirGaleria]. Decoracion ficticia del mockup NO implementada (DESIGN.md
 * R4 / non-goals): flash, modos Auto/Manual, deteccion de luz, chips OCR.
 */
@Composable
fun CameraEncuadre(
    onCapturado: (ByteArray, CropRegion) -> Unit,
    onCapturaFallida: (String) -> Unit,
    onCerrar: () -> Unit,
    onElegirGaleria: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    // La captura solo se habilita cuando la camara ya esta vinculada al
    // preview; pulsar antes evita errores de ImageCapture sin camara activa.
    var camaraLista by remember { mutableStateOf(false) }
    // Tamano real de la vista del encuadre: la misma geometria que pinta la
    // reticula calcula el recorte de la foto (WYSIWYG).
    var vistaSize by remember { mutableStateOf(IntSize.Zero) }

    DisposableEffect(lifecycleOwner) {
        camaraLista = false
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var disposeado = false
        val listener = Runnable {
            if (!disposeado) runCatching {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture,
                )
                camaraLista = true
            }.onFailure {
                // Sin camara utilizable: estado de error sin crash (escenario 6).
                onCapturaFallida("No se pudo abrir la cámara. Inténtalo de nuevo.")
            }
        }
        val executor = ContextCompat.getMainExecutor(context)
        providerFuture.addListener(listener, executor)
        onDispose {
            disposeado = true
            camaraLista = false
            runCatching { providerFuture.get().unbindAll() }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { vistaSize = it }
            .background(Color.Black)
            .testTag("captura_encuadre"),
    ) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        ReticulaCaptura(modifier = Modifier.fillMaxSize())

        if (!camaraLista) {
            // Estado de carga de la camara (sincronizacion del E2E de captura).
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .testTag("captura_camara_cargando"),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PildoraHint(texto = "Alinea los bordes del ticket dentro del recuadro")
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                // Controles dentro de la columna única; el visor sigue a todo el ancho.
                .widthIn(max = ANCHO_MAXIMO_CONTENIDO)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Galeria a la izquierda, simetrica a "Cerrar" respecto del obturador.
            BotonGaleria(onClick = onElegirGaleria)

            BotonObturador(
                enabled = camaraLista,
                onClick = {
                    capturar(
                        context = context,
                        imageCapture = imageCapture,
                        crop = cropDeLaVista(vistaSize, density),
                        onCapturado = onCapturado,
                        onCapturaFallida = onCapturaFallida,
                    )
                },
            )

            BotonCerrar(onClick = onCerrar)
        }
    }
}

/** Dispara [ImageCapture] y entrega los bytes JPEG crudos (con EXIF) y el recorte. */
private fun capturar(
    context: Context,
    imageCapture: ImageCapture,
    crop: CropRegion,
    onCapturado: (ByteArray, CropRegion) -> Unit,
    onCapturaFallida: (String) -> Unit,
) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        onCapturaFallida("Sin permiso de cámara")
        return
    }
    val stream = ByteArrayOutputStream()
    val opciones = ImageCapture.OutputFileOptions.Builder(stream).build()
    imageCapture.takePicture(
        opciones,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(results: ImageCapture.OutputFileResults) {
                onCapturado(stream.toByteArray(), crop)
            }

            override fun onError(exception: ImageCaptureException) {
                onCapturaFallida("No se pudo capturar la foto. Inténtalo de nuevo.")
            }
        },
    )
}

/**
 * Region del recuadro (mockup 04) en coordenadas de la vista, mapeable a la
 * imagen capturada: usa la MISMA geometria que pinta la reticula.
 */
private fun cropDeLaVista(size: IntSize, density: Density): CropRegion = with(density) {
    val ancho = size.width.toFloat().coerceAtLeast(1f)
    val alto = size.height.toFloat().coerceAtLeast(1f)
    val zona = EncuadreGeometria.zona(
        anchoVista = ancho,
        altoVista = alto,
        margenPx = EncuadreGeometria.MARGEN_DP.dp.toPx(),
        anchoMaxPx = EncuadreGeometria.ANCHO_MAX_DP.dp.toPx(),
        altoFrac = EncuadreGeometria.ALTO_FRAC,
        altoMaxPx = EncuadreGeometria.ALTO_MAX_DP.dp.toPx(),
    )
    val normalizado = EncuadreGeometria.rectNormalizado(zona, ancho, alto)
    CropRegion(
        left = normalizado.left,
        top = normalizado.top,
        right = normalizado.right,
        bottom = normalizado.bottom,
        viewAspectRatio = ancho / alto,
    )
}

/**
 * Reticula normativa del mockup 04: scrim ~65% fuera de la zona de captura,
 * marcas de esquina (24dp x 3dp, secondary-fixed) y sin decoracion ficticia.
 */
@Composable
private fun ReticulaCaptura(modifier: Modifier = Modifier) {
    val esquinaColor = MaterialTheme.colorScheme.secondaryFixed
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val zonaCalc = EncuadreGeometria.zona(
            anchoVista = size.width,
            altoVista = size.height,
            margenPx = EncuadreGeometria.MARGEN_DP.dp.toPx(),
            anchoMaxPx = EncuadreGeometria.ANCHO_MAX_DP.dp.toPx(),
            altoFrac = EncuadreGeometria.ALTO_FRAC,
            altoMaxPx = EncuadreGeometria.ALTO_MAX_DP.dp.toPx(),
        )
        val zona = Rect(zonaCalc.left, zonaCalc.top, zonaCalc.right, zonaCalc.bottom)
        val recorte = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(Offset.Zero, size))
            addRect(zona)
        }
        drawPath(path = recorte, color = Color.Black.copy(alpha = 0.65f))

        val largo = 24.dp.toPx()
        val grosor = 3.dp.toPx()
        val esquinas = listOf(
            Triple(zona.left, zona.top, 1 to 1),
            Triple(zona.right, zona.top, -1 to 1),
            Triple(zona.left, zona.bottom, 1 to -1),
            Triple(zona.right, zona.bottom, -1 to -1),
        )
        for ((x, y, direccion) in esquinas) {
            val (dx, dy) = direccion
            drawLine(
                color = esquinaColor,
                start = Offset(x, y),
                end = Offset(x + dx * largo, y),
                strokeWidth = grosor,
            )
            drawLine(
                color = esquinaColor,
                start = Offset(x, y),
                end = Offset(x, y + dy * largo),
                strokeWidth = grosor,
            )
        }
    }
}

/** Zona de captura proporcional al mockup 04 (max-w-xs, 68vh, centrada). */
@Composable
private fun PildoraHint(texto: String) {
    Row(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(9999.dp))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(9999.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(MaterialTheme.colorScheme.secondaryFixed, CircleShape),
        )
        Text(
            text = texto,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
        )
    }
}

/** Obturador circular 80dp del mockup 04 (anillo blanco + nucleo blanco). */
@Composable
private fun BotonObturador(
    onClick: () -> Unit,
    enabled: Boolean,
) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .border(3.dp, Color.White.copy(alpha = 0.9f), CircleShape)
            .padding(4.dp)
            .background(Color.White, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .testTag("captura_obturador"),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.PhotoCamera,
            contentDescription = "Capturar foto del ticket",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.graphicsLayer(alpha = if (enabled) 1f else 0.4f),
        )
    }
}

/** Elegir una foto de la galería (spec galeria-escaneo): espejo de [BotonCerrar], siempre habilitado. */
@Composable
private fun BotonGaleria(onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color.White.copy(alpha = 0.1f), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                .clickable(role = Role.Button, onClick = onClick)
                .testTag("captura_galeria"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoLibrary,
                contentDescription = "Elegir de la galería",
                tint = Color.White,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Galería",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f),
        )
    }
}

@Composable
private fun BotonCerrar(onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color.White.copy(alpha = 0.1f), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                .clickable(role = Role.Button, onClick = onClick)
                .testTag("captura_cerrar"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Cancelar captura",
                tint = Color.White,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Cerrar",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f),
        )
    }
}
