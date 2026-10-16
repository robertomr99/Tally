package com.robertomr.tally.feature.escanear

import app.cash.turbine.test
import com.robertomr.tally.domain.expenses.usecases.GuardarFotoCapturada
import com.robertomr.tally.domain.expenses.usecases.ReconocerTextoCrudo
import com.robertomr.tally.test.unit.CoroutineTestRule
import com.robertomr.tally.test.unit.FakeOcrEngine
import com.robertomr.tally.test.unit.FakePhotoFileStore
import com.robertomr.tally.test.unit.FakePhotoProcessor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Tests JVM de [EscanearViewModel] con Turbine, CoroutineTestRule y los dobles
 * de :test:unit (spec captura-foto, T9 + spec ocr-mlkit, T7): permiso,
 * procesando, revision, "repetir" borrando la foto anterior, y la cadena
 * guardar-foto -> OCR con el texto crudo registrado en el Borrador
 * (vacío/fallo sin bloquear, sin re-OCR, descarte en "Repetir foto").
 *
 * Las transiciones se observan con Turbine; cuando hace falta ver el estado
 * "Procesando" se retiene el doble del processor con un [CompletableDeferred]
 * para que la observacion sea determinista (sin carreras de conflation).
 */
class EscanearViewModelTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private val processor = FakePhotoProcessor(processedBytes = byteArrayOf(9, 9, 9))
    private val store = FakePhotoFileStore(savedName = "fotos/captura_test.jpg")
    private val engine = FakeOcrEngine(result = "MERCADONA\nTOTAL 12,34")
    private val viewModel = EscanearViewModel(
        guardarFotoCapturada = GuardarFotoCapturada(processor, store),
        reconocerTextoCrudo = ReconocerTextoCrudo(store, engine),
        photoFileStore = store,
    )

    @Test
    fun withoutPermissionStateStaysPending() = runTest {
        viewModel.estado.test {
            assertEquals(CapturaUiState.PermisoPendiente, awaitItem())

            viewModel.onImageCapturado(byteArrayOf(1))

            // Sin permiso el estado visible no cambia: se espera a que la
            // captura termine de procesarse y se comprueba que el StateFlow
            // sigue en PermisoPendiente (sin emisiones nuevas por conflation).
            esperarGuardado()
            expectNoEvents()
            assertEquals(CapturaUiState.PermisoPendiente, viewModel.estado.value)
        }
    }

    @Test
    fun grantingPermissionShowsEncuadre() = runTest {
        viewModel.estado.test {
            assertEquals(CapturaUiState.PermisoPendiente, awaitItem())

            viewModel.onPermisoCambiado(true)

            assertEquals(CapturaUiState.Encuadre, awaitItem())
        }
    }

    @Test
    fun capturingProcessesAndSavesThePhotoAtCaptureTime() = runTest {
        val crudos = byteArrayOf(1, 2, 3)
        processor.gate = CompletableDeferred()
        viewModel.onPermisoCambiado(true)

        viewModel.estado.test {
            assertEquals(CapturaUiState.Encuadre, awaitItem())

            viewModel.onImageCapturado(crudos)
            assertEquals(CapturaUiState.Procesando, awaitItem())

            processor.gate?.complete(Unit)
            val revision = awaitItem()
            assertTrue(revision is CapturaUiState.Revision)
            assertEquals("fotos/captura_test.jpg", (revision as CapturaUiState.Revision).rutaFoto)
        }

        // process -> save en el momento de la captura (bytes crudos al pipeline,
        // bytes procesados al store).
        assertEquals(1, processor.received.size)
        assertTrue(processor.received[0].contentEquals(crudos))
        assertEquals(1, store.savedPayloads.size)
        assertTrue(store.savedPayloads[0].contentEquals(byteArrayOf(9, 9, 9)))
    }

    @Test
    fun processorErrorShowsErrorState() = runTest {
        processor.error = IllegalStateException("fallo del pipeline")
        processor.gate = CompletableDeferred()
        viewModel.onPermisoCambiado(true)

        viewModel.estado.test {
            assertEquals(CapturaUiState.Encuadre, awaitItem())

            viewModel.onImageCapturado(byteArrayOf(1))
            assertEquals(CapturaUiState.Procesando, awaitItem())

            processor.gate?.complete(Unit)
            val error = awaitItem()
            assertTrue(error is CapturaUiState.Error)
            assertEquals("fallo del pipeline", (error as CapturaUiState.Error).mensaje)
            assertTrue(store.savedPayloads.isEmpty())
        }
    }

    @Test
    fun cameraFailureShowsErrorStateWithoutSaving() = runTest {
        viewModel.onPermisoCambiado(true)

        viewModel.estado.test {
            assertEquals(CapturaUiState.Encuadre, awaitItem())

            viewModel.onCapturaFallida("La camara no pudo capturar")

            val error = awaitItem()
            assertTrue(error is CapturaUiState.Error)
            assertTrue(store.savedPayloads.isEmpty())
            assertTrue(processor.received.isEmpty())
        }
    }

    @Test
    fun repetirBorraLaFotoAnteriorYVuelveAlEncuadre() = runTest {
        viewModel.onPermisoCambiado(true)
        viewModel.onImageCapturado(byteArrayOf(1))
        val rutaGuardada = esperarRevision().rutaFoto

        viewModel.onRepetirFoto()

        // La foto del borrador reemplazado se borra en disco (best-effort).
        esperarBorrado("fotos/captura_test.jpg")
        assertEquals(listOf("fotos/captura_test.jpg"), store.deletedPaths)
        assertEquals(CapturaUiState.Encuadre, esperarEstado { it is CapturaUiState.Encuadre })

        // Puede capturarse otra: queda solo la nueva (el fake devuelve el mismo
        // nombre configurable; lo que se comprueba es que el flujo sigue vivo).
        viewModel.onImageCapturado(byteArrayOf(2))
        val segunda = esperarRevision().rutaFoto
        assertEquals(rutaGuardada, segunda)
    }

    // --- OCR (spec ocr-mlkit): texto crudo registrado con el Borrador ---

    @Test
    fun ocrTextIsRegisteredWithTheDraftTogetherWithThePhotoPath() = runTest {
        viewModel.onPermisoCambiado(true)
        viewModel.onImageCapturado(byteArrayOf(1))

        val revision = esperarRevision()

        assertEquals("MERCADONA\nTOTAL 12,34", revision.textoCrudo)
        assertEquals(false, revision.ocrNoDisponible)
        // El OCR recibe los bytes JPEG YA GUARDADOS de la foto (1 invocacion).
        assertEquals(1, engine.received.size)
        assertTrue(engine.received[0].contentEquals(store.savedPayloads[0]))
    }

    @Test
    fun emptyOcrResultReachesRevisionAsEmptyTextWithoutBlocking() = runTest {
        engine.result = ""
        viewModel.onPermisoCambiado(true)

        viewModel.estado.test {
            assertEquals(CapturaUiState.Encuadre, awaitItem())

            viewModel.onImageCapturado(byteArrayOf(1))
            assertEquals(CapturaUiState.Procesando, awaitItem())

            val revision = awaitItem() as CapturaUiState.Revision
            // Escenario 3: OCR vacio -> texto "" y sin marca de fallo; la foto
            // NO se considera gastada (sigue en disco y en el borrador).
            assertEquals("", revision.textoCrudo)
            assertEquals(false, revision.ocrNoDisponible)
            assertEquals("fotos/captura_test.jpg", revision.rutaFoto)
            assertEquals(1, store.savedPayloads.size)
        }
    }

    @Test
    fun ocrFailureDoesNotBlockTheCaptureAndIsMarkedUnavailable() = runTest {
        engine.error = IllegalStateException("motor OCR caido")
        viewModel.onPermisoCambiado(true)

        viewModel.estado.test {
            assertEquals(CapturaUiState.Encuadre, awaitItem())

            viewModel.onImageCapturado(byteArrayOf(1))
            assertEquals(CapturaUiState.Procesando, awaitItem())

            val revision = awaitItem() as CapturaUiState.Revision
            // Escenario 4: el fallo del motor NO convierte la captura en error
            // (no hay CapturaUiState.Error) y la foto ya guardada se conserva.
            assertEquals("", revision.textoCrudo)
            assertEquals(true, revision.ocrNoDisponible)
            assertEquals(1, store.savedPayloads.size)
        }
    }

    @Test
    fun repeatedStateQueriesDoNotRerunTheOcr() = runTest {
        viewModel.onPermisoCambiado(true)
        viewModel.onImageCapturado(byteArrayOf(1))
        val revision = esperarRevision()

        // Escenario 6: el texto del borrador se recupera tal cual, una y otra
        // vez, sin nueva captura -> el motor registra UNA sola invocacion.
        repeat(10) {
            val actual = viewModel.estado.value as CapturaUiState.Revision
            assertEquals(revision.textoCrudo, actual.textoCrudo)
        }
        assertEquals(1, engine.received.size)
    }

    @Test
    fun repetirDescartaElTextoDelBorradorYLaSiguienteCapturaGeneraElSuyo() = runTest {
        engine.result = "TEXTO A"
        viewModel.onPermisoCambiado(true)
        viewModel.onImageCapturado(byteArrayOf(1))
        assertEquals("TEXTO A", esperarRevision().textoCrudo)

        // Escenario 5: "Repetir foto" descarta foto y texto del borrador.
        viewModel.onRepetirFoto()
        assertEquals(CapturaUiState.Encuadre, esperarEstado { it is CapturaUiState.Encuadre })

        // La captura siguiente genera su propio texto (no reutiliza el anterior).
        engine.result = "TEXTO B"
        viewModel.onImageCapturado(byteArrayOf(2))
        assertEquals("TEXTO B", esperarRevision().textoCrudo)
        assertEquals(2, engine.received.size)
    }

    private fun esperarEstado(esperado: (CapturaUiState) -> Boolean): CapturaUiState {
        val limite = System.currentTimeMillis() + 2_000
        while (System.currentTimeMillis() < limite) {
            val actual = viewModel.estado.value
            if (esperado(actual)) return actual
            Thread.sleep(10)
        }
        error("No se alcanzo el estado esperado; estado=${viewModel.estado.value}")
    }

    private fun esperarRevision(): CapturaUiState.Revision {
        val estado = esperarEstado { it is CapturaUiState.Revision }
        return estado as CapturaUiState.Revision
    }

    private fun esperarBorrado(ruta: String) {
        val limite = System.currentTimeMillis() + 2_000
        while (!store.deletedPaths.contains(ruta) && System.currentTimeMillis() < limite) {
            // El borrado va en Dispatchers.IO (best-effort); espera acotada.
            Thread.sleep(10)
        }
    }

    private fun esperarGuardado() {
        val limite = System.currentTimeMillis() + 2_000
        while (store.savedPayloads.isEmpty() && System.currentTimeMillis() < limite) {
            Thread.sleep(10)
        }
    }
}
