package com.robertomr.tally.framework.database

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.robertomr.tally.domain.expenses.data.CropRegion
import com.robertomr.tally.framework.database.source.CapturedPhotoProcessor
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentados del pipeline de foto (spec captura-foto, T8 /
 * verificacion 2 de la feature): con la imagen de recursos girada por EXIF
 * (Orientation=6) la salida queda con los pixeles rotados (dimensiones
 * intercambiadas), el lado largo <= 1280 px y sin tag de orientacion.
 *
 * El fixture `ticket_exif_rotado.jpg` (1600x900, EXIF Orientation=6) lleva un
 * cuadro rojo en la esquina inferior izquierda del fichero que, tras la
 * correccion de 90 CW, debe quedar en la esquina superior izquierda visible.
 */
@RunWith(AndroidJUnit4::class)
class CapturedPhotoProcessorTest {

    private val processor = CapturedPhotoProcessor()
    private val instrumentacion = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()

    @Test
    fun fixtureHasExifOrientation6AndLandscapeDims() {
        val raw = leerFixture()
        val exif = ExifInterface(ByteArrayInputStream(raw))
        val entrada = BitmapFactory.decodeByteArray(raw, 0, raw.size)

        assertEquals(6, exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1))
        assertEquals(1600, entrada.width)
        assertEquals(900, entrada.height)
    }

    @Test
    fun processBakesExifRotationIntoPixelsAndDropsOrientationTag() = runBlocking {
        val raw = leerFixture()
        val entrada = BitmapFactory.decodeByteArray(raw, 0, raw.size)

        val salidaBytes = processor.process(raw, fotogramaCompleto(900f, 1600f))
        val salida = BitmapFactory.decodeByteArray(salidaBytes, 0, salidaBytes.size)

        println(
            "CapturedPhotoProcessorTest: entrada=${entrada.width}x${entrada.height} " +
                "salida=${salida.width}x${salida.height}"
        )

        // Dimensiones intercambiadas (girada 90) y lado largo en 1280 px.
        assertEquals(720, salida.width)
        assertEquals(1280, salida.height)
        assertEquals(1280, maxOf(salida.width, salida.height))
        assertTrue(salida.width < entrada.width)
        assertTrue(salida.height > entrada.height)

        // Sin tag de orientacion EXIF en la salida (Bitmap.compress no escribe EXIF).
        val exifSalida = ExifInterface(ByteArrayInputStream(salidaBytes))
        assertEquals(
            ExifInterface.ORIENTATION_UNDEFINED,
            exifSalida.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED),
        )

        // La rotacion va en el sentido correcto: el marcador rojo del fixture
        // (esquina inferior izquierda del fichero) queda arriba a la izquierda.
        val marcador = salida.getPixel(30, 30)
        assertTrue("pixel(30,30)=$marcador no es rojo", Color.red(marcador) > 180 && Color.green(marcador) < 100)
        val opuesto = salida.getPixel(salida.width - 30, salida.height - 30)
        val opuestoEsRojo = Color.red(opuesto) > 180 && Color.green(opuesto) < 100
        assertFalse("pixel opuesto=$opuesto no deberia ser rojo", opuestoEsRojo)
    }

    @Test
    fun processScalesLargeJpegWithoutExifTo1280AndDoesNotRotate() = runBlocking {
        val raw = jpegSintetico(2000, 1000)

        val salidaBytes = processor.process(raw, fotogramaCompleto(2000f, 1000f))
        val salida = BitmapFactory.decodeByteArray(salidaBytes, 0, salidaBytes.size)

        println("CapturedPhotoProcessorTest: sintetico 2000x1000 -> ${salida.width}x${salida.height}")

        assertEquals(1280, salida.width)
        assertEquals(640, salida.height)

        val exifSalida = ExifInterface(ByteArrayInputStream(salidaBytes))
        assertEquals(
            ExifInterface.ORIENTATION_UNDEFINED,
            exifSalida.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED),
        )
    }

    @Test
    fun processDoesNotScaleSmallJpeg() = runBlocking {
        val raw = jpegSintetico(800, 600)

        val salidaBytes = processor.process(raw, fotogramaCompleto(800f, 600f))
        val salida = BitmapFactory.decodeByteArray(salidaBytes, 0, salidaBytes.size)

        assertEquals(800, salida.width)
        assertEquals(600, salida.height)
    }

    @Test
    fun processCropsTheGuidedRegionInsteadOfTheWholeFrame() = runBlocking {
        val raw = jpegSintetico(2000, 1000)

        // Recuadro centrado al 50% de la vista, con el mismo aspecto que la
        // imagen: el recorte queda 1000x500 (no el fotograma completo).
        val mittad = CropRegion(0.25f, 0.25f, 0.75f, 0.75f, viewAspectRatio = 2f)

        val salidaBytes = processor.process(raw, mittad)
        val salida = BitmapFactory.decodeByteArray(salidaBytes, 0, salidaBytes.size)

        println("CapturedPhotoProcessorTest: recorte 2000x1000 -> ${salida.width}x${salida.height}")

        assertEquals(1000, salida.width)
        assertEquals(500, salida.height)
        assertEquals(1000, maxOf(salida.width, salida.height))
    }

    // --- Spec galeria-escaneo: imagen elegida de la galeria (CropRegion.Completo) ---

    @Test
    fun completoConservaLaImagenEnteraDeUn12MpConExif6() = runBlocking {
        // 4000x3000 (12 MP) apaisado con EXIF 6: debe salir vertical 960x1280,
        // completo (sin recorte) y sin tag de orientacion.
        val raw = jpegConExif(4000, 3000, ExifInterface.ORIENTATION_ROTATE_90)

        val salidaBytes = processor.process(raw, CropRegion.Completo)
        val salida = BitmapFactory.decodeByteArray(salidaBytes, 0, salidaBytes.size)

        println("CapturedPhotoProcessorTest: galeria 4000x3000 EXIF6 -> ${salida.width}x${salida.height}")

        assertEquals(960, salida.width)
        assertEquals(1280, salida.height)
        val exifSalida = ExifInterface(ByteArrayInputStream(salidaBytes))
        assertEquals(
            ExifInterface.ORIENTATION_UNDEFINED,
            exifSalida.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED),
        )
    }

    @Test
    fun completoConExif8TambienQuedaVertical() = runBlocking {
        val raw = jpegConExif(2000, 1000, ExifInterface.ORIENTATION_ROTATE_270)

        val salidaBytes = processor.process(raw, CropRegion.Completo)
        val salida = BitmapFactory.decodeByteArray(salidaBytes, 0, salidaBytes.size)

        assertEquals(640, salida.width)
        assertEquals(1280, salida.height)
    }

    @Test
    fun completoSinExifNoRecortaNiGira() = runBlocking {
        val salidaBytes = processor.process(jpegSintetico(2000, 1000), CropRegion.Completo)
        val salida = BitmapFactory.decodeByteArray(salidaBytes, 0, salidaBytes.size)

        assertEquals(1280, salida.width)
        assertEquals(640, salida.height)
    }

    @Test
    fun decodificaPngYWebPDeLaGaleria() = runBlocking {
        for (formato in listOf(Bitmap.CompressFormat.PNG, Bitmap.CompressFormat.WEBP_LOSSY)) {
            val raw = bitmapComprimido(1600, 800, formato)

            val salidaBytes = processor.process(raw, CropRegion.Completo)
            val salida = BitmapFactory.decodeByteArray(salidaBytes, 0, salidaBytes.size)

            assertEquals("formato $formato", 1280, salida.width)
            assertEquals("formato $formato", 640, salida.height)
        }
    }

    @Test
    fun bytesQueNoSonUnaImagenLanzanUnErrorClaro() {
        val basura = ByteArray(4096) { (it * 31).toByte() }

        val error = runCatching { runBlocking { processor.process(basura, CropRegion.Completo) } }
            .exceptionOrNull()

        assertTrue("error=$error", error is IllegalStateException)
        assertEquals("La imagen no es válida", error?.message)
    }

    private fun bitmapComprimido(ancho: Int, alto: Int, formato: Bitmap.CompressFormat): ByteArray {
        val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(Color.rgb(200, 120, 40))
        val salida = ByteArrayOutputStream()
        bitmap.compress(formato, 90, salida)
        bitmap.recycle()
        return salida.toByteArray()
    }

    /** JPEG sintetico al que se le escribe el tag EXIF de orientacion. */
    private fun jpegConExif(ancho: Int, alto: Int, orientacion: Int): ByteArray {
        val fichero = java.io.File.createTempFile("galeria_", ".jpg", instrumentacion.targetContext.cacheDir)
        try {
            fichero.writeBytes(jpegSintetico(ancho, alto))
            ExifInterface(fichero.absolutePath).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, orientacion.toString())
                saveAttributes()
            }
            return fichero.readBytes()
        } finally {
            fichero.delete()
        }
    }

    private fun fotogramaCompleto(ancho: Float, alto: Float) =
        CropRegion(0f, 0f, 1f, 1f, viewAspectRatio = ancho / alto)

    private fun leerFixture(): ByteArray =
        instrumentacion.context.assets.open("ticket_exif_rotado.jpg").use { it.readBytes() }

    /** JPEG sintetico sin EXIF, con un degradado sencillo para que comprima de verdad. */
    private fun jpegSintetico(ancho: Int, alto: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        val lienzo = Canvas(bitmap)
        val pintura = Paint()
        for (x in 0 until ancho step 40) {
            pintura.color = if ((x / 40) % 2 == 0) 0xFF333333.toInt() else 0xFFBBBBBB.toInt()
            lienzo.drawRect(x.toFloat(), 0f, (x + 40).toFloat(), alto.toFloat(), pintura)
        }
        val salida = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, salida)
        bitmap.recycle()
        return salida.toByteArray()
    }
}
