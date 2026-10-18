package com.robertomr.tally.ajustes

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.robertomr.tally.MainActivity
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// E2E de la feature descarga-modelo (spec, T8) en emulator-5554: Ajustes desde
// Resumen con la seccion "Modelo local", politica WiFi (red movil simulada con
// `svc wifi disable; svc data enable`: aviso y NINGUN trabajo encolado) y, con
// WiFi, arranque real del worker (sin token el modelo gated responde 401 y el
// estado pasa a Error con la guia de licencia). No se descarga el modelo entero
// en CI: la descarga completa y la reanudacion se verifican en la Tab S9+ y en
// los tests JVM del motor.
@RunWith(AndroidJUnit4::class)
class AjustesModeloFlujoTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val contexto: Context = instrumentation.targetContext

    @Before
    fun limpiar() {
        WorkManager.getInstance(contexto).cancelUniqueWork(TRABAJO).result.get()
        // Descarta trabajos terminados (un FAILED previo seguiria mostrandose como Error).
        WorkManager.getInstance(contexto).pruneWork().result.get()
        borrarFicherosDelModelo()
        shell("svc wifi enable")
    }

    @After
    fun restaurar() {
        shell("svc wifi enable")
        WorkManager.getInstance(contexto).cancelUniqueWork(TRABAJO).result.get()
        // Descarta trabajos terminados (un FAILED previo seguiria mostrandose como Error).
        WorkManager.getInstance(contexto).pruneWork().result.get()
        borrarFicherosDelModelo()
    }

    @Test
    fun ajustesDesdeResumenMuestraModeloLocalNoDescargado() {
        abrirAjustes()

        composeRule.onNodeWithTag("seccion_modelo_local").assertIsDisplayed()
        composeRule.onNodeWithTag("modelo_estado").assertTextEquals("No descargado")
        composeRule.onNodeWithTag("boton_descargar_modelo").assertIsDisplayed()
        composeRule.onNodeWithTag("token_estado").assertIsDisplayed()

        composeRule.onNodeWithTag("ajustes_volver").performClick()
        composeRule.onNodeWithTag("pantalla_resumen").assertIsDisplayed()
    }

    @Test
    fun conRedMovilAvisaYNoEncolaNingunTrabajo() {
        shell("svc wifi disable")
        shell("svc data enable")
        assertTrue("La red activa sigue sin coste tras desactivar WiFi", esperarRedMedida())
        try {
            abrirAjustes()
            composeRule.onNodeWithTag("modelo_estado").assertTextEquals("No descargado")

            composeRule.onNodeWithTag("boton_descargar_modelo").performClick()

            composeRule.waitUntil(10_000) {
                runCatching {
                    composeRule.onNodeWithTag("modelo_aviso")
                        .assertTextEquals("Solo se descarga con WiFi")
                }.isSuccess
            }
            composeRule.onNodeWithTag("modelo_estado").assertTextEquals("No descargado")
            val infos = WorkManager.getInstance(contexto)
                .getWorkInfosForUniqueWork(TRABAJO).get()
            assertTrue("No debe haber trabajo encolado: $infos", infos.isEmpty())
            assertFalse(File(directorioModelo(), "$FICHERO.part").exists())
        } finally {
            shell("svc wifi enable")
        }
    }

    @Test
    fun conWifiElWorkerArrancaYSinTokenAvisaDeLaLicencia() {
        assertTrue("Se necesita una red no medida (WiFi) en el emulador", redSinCoste())
        abrirAjustes()

        composeRule.onNodeWithTag("boton_descargar_modelo").performClick()

        // Sale de "No descargado": el trabajo se encolo y el worker arranco.
        composeRule.waitUntil(30_000) {
            runCatching {
                composeRule.onNodeWithTag("modelo_estado").assertTextEquals("No descargado")
            }.isFailure
        }
        val infos = WorkManager.getInstance(contexto).getWorkInfosForUniqueWork(TRABAJO).get()
        assertEquals(1, infos.size)

        // Con red real a Hugging Face y sin token: 401 -> Error + guia de licencia.
        val llegoAError = runCatching {
            composeRule.waitUntil(60_000) {
                runCatching {
                    composeRule.onNodeWithTag("modelo_estado").assertTextEquals("Error")
                }.isSuccess
            }
        }.isSuccess
        if (llegoAError) {
            composeRule.onNodeWithTag("modelo_detalle")
                .assertTextContains("Acepta la licencia en Hugging Face y revisa tu token")
            assertEquals(WorkInfo.State.FAILED, WorkManager.getInstance(contexto)
                .getWorkInfosForUniqueWork(TRABAJO).get().single().state)
        } else {
            // Sin acceso a Hugging Face desde el emulador: la descarga queda viva y se cancela.
            composeRule.onNodeWithTag("boton_cancelar_descarga").performClick()
            composeRule.waitUntil(10_000) {
                runCatching {
                    composeRule.onNodeWithTag("modelo_estado").assertTextEquals("No descargado")
                }.isSuccess
            }
        }
    }

    private fun abrirAjustes() {
        composeRule.onNodeWithTag("boton_ajustes").performClick()
        composeRule.onNodeWithTag("pantalla_ajustes").assertIsDisplayed()
        // El estado deja de ser "Comprobando..." en cuanto llega el primer valor.
        composeRule.waitUntil(10_000) {
            runCatching { composeRule.onNodeWithTag("modelo_estado").assertIsDisplayed() }.isSuccess
        }
    }

    private fun redSinCoste(): Boolean {
        val cm = contexto.getSystemService(ConnectivityManager::class.java)
        val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    private fun esperarRedMedida(): Boolean {
        val limite = System.currentTimeMillis() + 30_000
        while (System.currentTimeMillis() < limite) {
            if (!redSinCoste()) return true
            Thread.sleep(500)
        }
        return false
    }

    private fun directorioModelo(): File =
        File(contexto.getExternalFilesDir(null) ?: contexto.filesDir, "models")

    private fun borrarFicherosDelModelo() {
        File(directorioModelo(), FICHERO).delete()
        File(directorioModelo(), "$FICHERO.part").delete()
    }

    private fun shell(comando: String) {
        val pfd = instrumentation.uiAutomation.executeShellCommand(comando)
        android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
    }

    private companion object {
        const val TRABAJO = "modelo-descarga"
        const val FICHERO = "gemma-3n-E2B-it-int4.litertlm"
    }
}
