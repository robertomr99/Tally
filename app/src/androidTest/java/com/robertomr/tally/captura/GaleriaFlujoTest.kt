package com.robertomr.tally.captura

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.Intents.intending
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.robertomr.tally.MainActivity
import java.io.ByteArrayOutputStream
import java.io.File
import org.hamcrest.Matchers.anyOf
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

// E2E enfocado de la feature galeria-escaneo: elegir una foto de la galeria en
// Escanear (con y sin permiso de camara) y recorrer el mismo flujo que una
// captura. El selector del sistema se sustituye por un resultado simulado con
// espresso-intents (el Intent del Photo Picker se intercepta, nunca se abre);
// la imagen de prueba se crea en MediaStore con el contexto de la propia app,
// que es su propietaria y la puede leer sin permisos.
//
// El caso "sin permiso de camara" solo es valido sobre el estado limpio del
// permiso (no se revoca en caliente: Android mata el proceso de la app bajo
// test, ver CapturaFlujoTest); se salta con el motivo si CAMERA ya esta
// concedida y corre el primero (@FixMethodOrder).
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class GaleriaFlujoTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val reglas = composeRule

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private var imagenCreada: Uri? = null

    @Before
    fun prepararIntents() {
        Intents.init()
    }

    @After
    fun limpiar() {
        Intents.release()
        imagenCreada?.let { runCatching { contexto.contentResolver.delete(it, null, null) } }
    }

    @Test
    fun a_sinPermisoDeCamaraLaGaleriaLlegaARevisionYRepetirVuelveAlPermiso() {
        assumeTrue(
            "CAMERA ya concedida: no se puede revocar en caliente (Android mata el " +
                "proceso bajo test). Verificado con permiso denegado en instalacion " +
                "limpia del permiso (ver docs/evidence/galeria-escaneo/README.md).",
            !hayPermisoDeCamara(),
        )
        simularSelector(Activity.RESULT_OK, crearImagenEnMediaStore())

        irAEscanear()
        composeRule.onNodeWithTag("captura_permiso").assertIsDisplayed()
        composeRule.onNodeWithTag("permiso_galeria").assertIsDisplayed()
        composeRule.onNodeWithTag("permiso_galeria").performClick()

        esperarTag("captura_revision")
        composeRule.onNodeWithTag("captura_foto").assertIsDisplayed()
        // No se pidio ningun permiso.
        check(!hayPermisoDeCamara()) { "la galería no debe conceder ni pedir la cámara" }

        composeRule.onNodeWithTag("captura_repetir").performClick()
        composeRule.onNodeWithTag("captura_permiso").assertIsDisplayed()
    }

    @Test
    fun b_conPermisoLaGaleriaLlegaARevisionYRepetirVuelveAlEncuadre() {
        concederPermisoCamara()
        simularSelector(Activity.RESULT_OK, crearImagenEnMediaStore())

        irAEscanear()
        composeRule.onNodeWithTag("captura_encuadre").assertIsDisplayed()
        composeRule.onNodeWithTag("captura_galeria").assertIsDisplayed()
        composeRule.onNodeWithTag("captura_galeria").performClick()

        esperarTag("captura_revision")
        composeRule.onNodeWithTag("captura_foto").assertIsDisplayed()
        composeRule.onNodeWithTag("captura_continuar").assertIsDisplayed()

        composeRule.onNodeWithTag("captura_repetir").performClick()
        composeRule.onNodeWithTag("captura_encuadre").assertIsDisplayed()
    }

    @Test
    fun c_cancelarElSelectorNoCambiaNada() {
        concederPermisoCamara()
        simularSelector(Activity.RESULT_CANCELED, null)

        irAEscanear()
        composeRule.onNodeWithTag("captura_galeria").performClick()
        composeRule.waitForIdle()
        Thread.sleep(1_000)

        composeRule.onNodeWithTag("captura_encuadre").assertIsDisplayed()
        check(composeRule.onAllNodesWithTag("captura_error").fetchSemanticsNodes().isEmpty())
        check(composeRule.onAllNodesWithTag("captura_procesando").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun d_imagenIlegibleMuestraErrorYVolverRegresaAlEncuadre() {
        concederPermisoCamara()
        // Uri valido pero sin fichero detras: no se puede abrir.
        simularSelector(Activity.RESULT_OK, Uri.parse("content://media/external/images/media/999999999"))

        irAEscanear()
        composeRule.onNodeWithTag("captura_galeria").performClick()

        esperarTag("captura_error")
        composeRule.onNodeWithTag("captura_error").assertIsDisplayed()
        composeRule.onNodeWithTag("captura_reintentar").performClick()
        composeRule.onNodeWithTag("captura_encuadre").assertIsDisplayed()
    }

    // --- utilidades ---

    /** Intercepta el Intent del Photo Picker (y sus alternativas) con un resultado simulado. */
    private fun simularSelector(codigo: Int, uri: Uri?) {
        val respuesta = ActivityResult(
            codigo,
            if (uri != null) Intent().setData(uri) else Intent(),
        )
        intending(
            anyOf(
                hasAction(MediaStore.ACTION_PICK_IMAGES),
                hasAction(Intent.ACTION_OPEN_DOCUMENT),
                hasAction(ActivityResultContracts.PickVisualMedia.ACTION_SYSTEM_FALLBACK_PICK_IMAGES),
                hasAction("com.google.android.gms.provider.action.PICK_IMAGES"),
            )
        ).respondWith(respuesta)
    }

    /** JPEG sintetico apaisado con EXIF 6, guardado en MediaStore (propiedad de la app). */
    private fun crearImagenEnMediaStore(): Uri {
        val temporal = File.createTempFile("galeria_test_", ".jpg", contexto.cacheDir)
        try {
            val bitmap = Bitmap.createBitmap(1600, 900, Bitmap.Config.ARGB_8888)
            val lienzo = Canvas(bitmap)
            lienzo.drawColor(Color.WHITE)
            val pintura = Paint().apply {
                color = Color.BLACK
                textSize = 80f
            }
            lienzo.drawText("TICKET DE PRUEBA 12,34", 80f, 450f, pintura)
            val salida = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, salida)
            bitmap.recycle()
            temporal.writeBytes(salida.toByteArray())
            ExifInterface(temporal.absolutePath).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
                saveAttributes()
            }

            val valores = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "tally_galeria_test_${System.nanoTime()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/TallyTest")
            }
            val uri = contexto.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                valores,
            ) ?: error("No se pudo insertar la imagen de prueba en MediaStore")
            contexto.contentResolver.openOutputStream(uri)!!.use { it.write(temporal.readBytes()) }
            imagenCreada = uri
            return uri
        } finally {
            temporal.delete()
        }
    }

    private fun irAEscanear() {
        composeRule.onNodeWithTag("bottom_bar_escanear").performClick()
        composeRule.waitForIdle()
    }

    private fun esperarTag(tag: String) {
        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun concederPermisoCamara() {
        ejecutarShell("pm grant com.robertomr.tally android.permission.CAMERA")
        composeRule.waitForIdle()
    }

    private fun hayPermisoDeCamara(): Boolean =
        androidx.core.content.ContextCompat.checkSelfPermission(
            contexto,
            android.Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED

    private fun ejecutarShell(comando: String) {
        val fd = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(comando)
        ParcelFileDescriptor.AutoCloseInputStream(fd).use { it.readBytes() }
    }

    private companion object {
        const val TIMEOUT_MS = 20_000L
    }
}
