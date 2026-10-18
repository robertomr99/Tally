package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.entities.ModeloLocal
import com.robertomr.tally.domain.expenses.entities.ResultadoDescarga
import com.robertomr.tally.test.unit.FakeDownloadConditions
import com.robertomr.tally.test.unit.FakeModelAccessTokenStore
import com.robertomr.tally.test.unit.FakeModelDownloader
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Casos de uso de la descarga del modelo (spec descarga-modelo, escenarios 3-5). */
class DescargarModeloTest {

    private val downloader = FakeModelDownloader()
    private val conditions = FakeDownloadConditions()
    private val descargar = DescargarModelo(downloader, conditions)

    private val completo = ModeloLocal.TAMANO_ESPERADO_BYTES + ModeloLocal.MARGEN_ESPACIO_BYTES

    @Test
    fun sinRedSinCosteDevuelveRequiereWifiYNoInicia() = runTest {
        conditions.redSinCoste = false

        assertEquals(ResultadoDescarga.RequiereWifi, descargar())
        assertEquals(0, downloader.iniciarCount)
    }

    @Test
    fun laPoliticaWifiSeComprueba_antesQueElEspacio() = runTest {
        conditions.redSinCoste = false
        conditions.espacioLibre = 0L

        assertEquals(ResultadoDescarga.RequiereWifi, descargar())
    }

    @Test
    fun espacioInsuficienteDevuelveNecesariosYLibresSinIniciar() = runTest {
        conditions.espacioLibre = completo - 1

        assertEquals(ResultadoDescarga.EspacioInsuficiente(completo, completo - 1), descargar())
        assertEquals(0, downloader.iniciarCount)
    }

    @Test
    fun elEspacioDescuentaLoYaDescargado() = runTest {
        conditions.bytesYaDescargados = 3_000_000_000L
        val necesarios = ModeloLocal.TAMANO_ESPERADO_BYTES - 3_000_000_000L +
            ModeloLocal.MARGEN_ESPACIO_BYTES
        conditions.espacioLibre = necesarios

        assertEquals(ResultadoDescarga.Iniciada, descargar())
        assertEquals(1, downloader.iniciarCount)

        conditions.espacioLibre = necesarios - 1
        assertEquals(
            ResultadoDescarga.EspacioInsuficiente(necesarios, necesarios - 1),
            descargar(),
        )
    }

    @Test
    fun conWifiYEspacioInicia() = runTest {
        conditions.espacioLibre = completo

        assertEquals(ResultadoDescarga.Iniciada, descargar())
        assertEquals(1, downloader.iniciarCount)
    }

    @Test
    fun cancelarYEliminarDelegan() = runTest {
        CancelarDescargaModelo(downloader)()
        EliminarModelo(downloader)()

        assertEquals(1, downloader.cancelarCount)
        assertEquals(1, downloader.eliminarCount)
    }

    @Test
    fun observarEstadoReenviaElFlujoDelDownloader() = runTest {
        assertEquals(
            downloader.estadoFlow.value,
            ObservarEstadoModelo(downloader)().first(),
        )
    }

    @Test
    fun configurarTokenGuardaRecortadoYBorraSiEstaVacio() = runTest {
        val store = FakeModelAccessTokenStore()
        val configurar = ConfigurarTokenModelo(store)

        configurar("  hf_abc  ")
        assertEquals("hf_abc", store.token)
        assertTrue(ObservarTokenModelo(store)().first())

        configurar("   ")
        assertNull(store.token)
        assertFalse(ObservarTokenModelo(store)().first())
    }
}
