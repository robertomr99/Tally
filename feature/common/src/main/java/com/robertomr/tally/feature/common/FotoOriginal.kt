package com.robertomr.tally.feature.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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

/** Lado maximo (px) al que se muestrea la foto al decodificarla (no cargar el JPEG completo). */
private const val LADO_MAXIMO_PX = 2048

private sealed interface EstadoFoto {
    data object Cargando : EstadoFoto
    data class Imagen(val bitmap: ImageBitmap) : EstadoFoto
    data object Ausente : EstadoFoto
}

/**
 * Foto original de un ticket guardado (spec captura-foto / detalle-ticket): la
 * decodifica con `BitmapFactory` en `Dispatchers.IO` y con `inSampleSize` (sin
 * libreria de imagenes). `rutaFoto` es relativa bajo `filesDir`.
 *
 * Tres estados: spinner mientras decodifica, la imagen (`testTagImagen`) o, si
 * la ruta esta en blanco o el fichero falta/no se puede leer, una tarjeta
 * "Foto no disponible" (`testTagAusente`); nunca crashea.
 */
@Composable
fun FotoOriginal(
    rutaFoto: String,
    modifier: Modifier = Modifier,
    testTagImagen: String = "foto_original",
    testTagAusente: String = "foto_ausente",
    contentDescription: String = "Foto original del ticket",
) {
    val context = LocalContext.current
    val estado by produceState<EstadoFoto>(initialValue = EstadoFoto.Cargando, rutaFoto) {
        value = withContext(Dispatchers.IO) {
            val bitmap = decodificarFoto(File(context.filesDir, rutaFoto), rutaFoto)
            if (bitmap != null) EstadoFoto.Imagen(bitmap.asImageBitmap()) else EstadoFoto.Ausente
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (val actual = estado) {
            EstadoFoto.Cargando -> CircularProgressIndicator(modifier = Modifier.size(32.dp))
            // Relación de aspecto de la imagen real: el recorte y el borde envuelven la foto
            // visible y no todo el área disponible con barras vacías a los lados (V06).
            is EstadoFoto.Imagen -> Image(
                bitmap = actual.bitmap,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .aspectRatio(actual.bitmap.width.toFloat() / actual.bitmap.height.toFloat())
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .testTag(testTagImagen),
            )
            EstadoFoto.Ausente -> TarjetaFotoAusente(
                modifier = Modifier.testTag(testTagAusente),
            )
        }
    }
}

/** Tarjeta "Foto no disponible" (DESIGN.md: copy en espanol, sin decoracion). */
@Composable
fun TarjetaFotoAusente(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                RoundedCornerShape(16.dp),
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.BrokenImage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Foto no disponible",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * `true` si la foto existe como fichero legible bajo `filesDir`. Comprobacion
 * barata (sin decodificar) para decidir en el detalle entre "Ver foto original"
 * y la tarjeta de foto ausente. Suspende en `Dispatchers.IO`.
 */
suspend fun existeFoto(filesDir: File, rutaFoto: String): Boolean =
    withContext(Dispatchers.IO) {
        rutaFoto.isNotBlank() && File(filesDir, rutaFoto).let { it.isFile && it.canRead() }
    }

/** Decodifica con muestreo; `null` si la ruta esta en blanco, no existe o no decodifica. */
private fun decodificarFoto(fichero: File, rutaFoto: String): Bitmap? {
    if (rutaFoto.isBlank() || !fichero.isFile) return null
    return try {
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(fichero.absolutePath, limites)
        var muestreo = 1
        val ladoMayor = maxOf(limites.outWidth, limites.outHeight)
        while (ladoMayor / (muestreo * 2) >= LADO_MAXIMO_PX) muestreo *= 2
        val opciones = BitmapFactory.Options().apply { inSampleSize = muestreo }
        BitmapFactory.decodeFile(fichero.absolutePath, opciones)
    } catch (e: Exception) {
        null
    } catch (e: OutOfMemoryError) {
        null
    }
}
