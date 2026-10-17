package com.robertomr.tally.feature.escanear

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.robertomr.tally.domain.expenses.data.CropRegion
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.Linea
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.TicketExtraido
import com.robertomr.tally.domain.expenses.usecases.ExtraerTicket
import com.robertomr.tally.domain.expenses.usecases.GuardarFotoCapturada
import com.robertomr.tally.domain.expenses.usecases.ReconocerTextoCrudo
import com.robertomr.tally.test.unit.CoroutineTestRule
import com.robertomr.tally.test.unit.FakeOcrEngine
import com.robertomr.tally.test.unit.FakePhotoFileStore
import com.robertomr.tally.test.unit.FakePhotoProcessor
import com.robertomr.tally.test.unit.FakeTextEngine
import java.time.LocalDateTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Tests JVM de [EscanearViewModel] con Turbine, CoroutineTestRule y los dobles
 * de :test:unit (spec captura-foto, T9 + spec ocr-mlkit, T7 + spec
 * procesado-ui, T7): permiso, procesando, revision, "repetir" borrando la foto
 * anterior, la cadena guardar-foto -> OCR con el texto crudo registrado en el
 * Borrador (vacío/fallo sin bloquear, sin re-OCR, descarte en "Repetir foto") y
 * el procesado de la extraccion: pasos/estados reales, cancelacion con
 * [FakeTextEngine.gate] (aborta sin borrar la foto ni tocar `TicketRepository`),
 * error honesto, texto blank -> resultado rapido "0/5" sin invocar el motor y
 * badge con el motor real.
 *
 * Las transiciones se observan con Turbine; cuando hace falta ver un estado
 * intermedio ("Procesando" o "ProcesandoExtraccion") se retiene el doble con un
 * [CompletableDeferred] para que la observacion sea determinista (sin carreras
 * de conflation).
 */
class EscanearViewModelTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private val processor = FakePhotoProcessor(processedBytes = byteArrayOf(9, 9, 9))
    private val store = FakePhotoFileStore(savedName = "fotos/captura_test.jpg")
    private val crop = CropRegion(left = 0f, top = 0f, right = 1f, bottom = 1f, viewAspectRatio = 1f)
    private val engine = FakeOcrEngine(result = "MERCADONA\nTOTAL 12,34")
    private val textEngine = FakeTextEngine(
        result = TicketExtraido(
            comercio = "MERCADONA",
            fechaEmision = LocalDateTime.of(2026, 10, 15, 12, 0),
            total = 35.30,
            lineas = listOf(Linea("LECHE", 2, 1.05)),
            categoria = Categoria.ALIMENTACION,
            motor = Motor.NUBE,
        ),
    )
    private val viewModel = EscanearViewModel(
        guardarFotoCapturada = GuardarFotoCapturada(processor, store),
        reconocerTextoCrudo = ReconocerTextoCrudo(store, engine),
        extraerTicket = ExtraerTicket(textEngine),
        photoFileStore = store,
    )

    @Test
    fun withoutPermissionStateStaysPending() = runTest {
        viewModel.estado.test {
            assertEquals(CapturaUiState.PermisoPendiente, awaitItem())

            viewModel.onImageCapturado(byteArrayOf(1), crop)

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

            viewModel.onImageCapturado(crudos, crop)
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

            viewModel.onImageCapturado(byteArrayOf(1), crop)
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
        viewModel.onImageCapturado(byteArrayOf(1), crop)
        val rutaGuardada = esperarRevision().rutaFoto

        viewModel.onRepetirFoto()

        // La foto del borrador reemplazado se borra en disco (best-effort).
        esperarBorrado("fotos/captura_test.jpg")
        assertEquals(listOf("fotos/captura_test.jpg"), store.deletedPaths)
        assertEquals(CapturaUiState.Encuadre, esperarEstado { it is CapturaUiState.Encuadre })

        // Puede capturarse otra: queda solo la nueva (el fake devuelve el mismo
        // nombre configurable; lo que se comprueba es que el flujo sigue vivo).
        viewModel.onImageCapturado(byteArrayOf(2), crop)
        val segunda = esperarRevision().rutaFoto
        assertEquals(rutaGuardada, segunda)
    }

    // --- OCR (spec ocr-mlkit): texto crudo registrado con el Borrador ---

    @Test
    fun ocrTextIsRegisteredWithTheDraftTogetherWithThePhotoPath() = runTest {
        viewModel.onPermisoCambiado(true)
        viewModel.onImageCapturado(byteArrayOf(1), crop)

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
        // Se retiene el pipeline de la foto para observar el estado
        // "Procesando" sin carreras de conflation (mismo patron que
        // capturingProcessesAndSavesThePhotoAtCaptureTime).
        processor.gate = CompletableDeferred()
        viewModel.onPermisoCambiado(true)

        viewModel.estado.test {
            assertEquals(CapturaUiState.Encuadre, awaitItem())

            viewModel.onImageCapturado(byteArrayOf(1), crop)
            assertEquals(CapturaUiState.Procesando, awaitItem())

            processor.gate?.complete(Unit)
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
        // Patron de observacion determinista de "Procesando" (sin conflation).
        processor.gate = CompletableDeferred()
        viewModel.onPermisoCambiado(true)

        viewModel.estado.test {
            assertEquals(CapturaUiState.Encuadre, awaitItem())

            viewModel.onImageCapturado(byteArrayOf(1), crop)
            assertEquals(CapturaUiState.Procesando, awaitItem())

            processor.gate?.complete(Unit)
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
        viewModel.onImageCapturado(byteArrayOf(1), crop)
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
        viewModel.onImageCapturado(byteArrayOf(1), crop)
        assertEquals("TEXTO A", esperarRevision().textoCrudo)

        // Escenario 5: "Repetir foto" descarta foto y texto del borrador.
        viewModel.onRepetirFoto()
        assertEquals(CapturaUiState.Encuadre, esperarEstado { it is CapturaUiState.Encuadre })

        // La captura siguiente genera su propio texto (no reutiliza el anterior).
        engine.result = "TEXTO B"
        viewModel.onImageCapturado(byteArrayOf(2), crop)
        assertEquals("TEXTO B", esperarRevision().textoCrudo)
        assertEquals(2, engine.received.size)
    }

    // --- Procesado (spec procesado-ui): extraccion, cancelacion, error ---

    @Test
    fun continuarAbreElProcesadoYTerminaEnElResultadoConCompletitud() = runTest {
        textEngine.gate = CompletableDeferred()
        viewModel.onPermisoCambiado(true)
        viewModel.onImageCapturado(byteArrayOf(1), crop)
        val revision = esperarRevision()

        viewModel.estado.test {
            assertEquals(revision, awaitItem())

            viewModel.onContinuar()

            // Procesado en curso: mismo Borrador (foto + texto crudo) y el
            // motor corriendo (spec T3: pasos Encuadre/OCR Completados porque
            // ya ocurrieron en la captura; Categorizacion es el paso activo).
            val procesando = awaitItem() as CapturaUiState.ProcesandoExtraccion
            assertEquals(revision.rutaFoto, procesando.rutaFoto)
            assertEquals(revision.textoCrudo, procesando.textoCrudo)

            textEngine.gate?.complete(Unit)
            val lista = awaitItem() as CapturaUiState.ExtraccionLista
            assertEquals(textEngine.result, lista.extraido)
            // Completitud real de campos (CONTEXT.md), nunca % de confianza.
            assertEquals("5/5 campos extra\u00eddos", etiquetaCompletitud(lista.extraido))
            expectNoEvents()
        }

        assertEquals(listOf("MERCADONA\nTOTAL 12,34"), textEngine.received)
    }

    @Test
    fun cancelarAbortaLaExtraccionSinBorrarLaFotoYConElBorradorIntacto() = runTest {
        textEngine.gate = CompletableDeferred()
        viewModel.onPermisoCambiado(true)
        viewModel.onImageCapturado(byteArrayOf(1), crop)
        val revision = esperarRevision()

        viewModel.estado.test {
            assertEquals(revision, awaitItem())

            viewModel.onContinuar()
            assertTrue(awaitItem() is CapturaUiState.ProcesandoExtraccion)
            // El motor ya estaba corriendo (retenido en el gate).
            assertEquals(1, textEngine.received.size)

            // Cancelar (o back del sistema) aborta la corrutina: jamas se
            // convierte en error ni en resultado (R-PROC4).
            viewModel.onCancelarProcesado()
            val trasCancelar = awaitItem()
            assertEquals(revision, trasCancelar)
            expectNoEvents()

            // "Continuar" sigue reintentable sobre el mismo Borrador.
            viewModel.onContinuar()
            assertTrue(awaitItem() is CapturaUiState.ProcesandoExtraccion)
            textEngine.gate?.complete(Unit)
            assertTrue(awaitItem() is CapturaUiState.ExtraccionLista)
        }

        // Sin datos huerfanos: la foto NO se borra (solo "Repetir foto" la
        // borra) y este ViewModel ni recibe `TicketRepository` (cero escrituras
        // posibles en Room; non-goal de la feature).
        assertTrue("la foto debe conservarse al cancelar", store.deletedPaths.isEmpty())
        assertEquals(2, textEngine.received.size)
    }

    @Test
    fun elErrorDeExtraccionSeMuestraSinInventarDatosYReintentaConElMismoMotor() = runTest {
        textEngine.error = IllegalStateException("Firebase AI no configurado")
        textEngine.gate = CompletableDeferred()
        viewModel.onPermisoCambiado(true)
        viewModel.onImageCapturado(byteArrayOf(1), crop)
        esperarRevision()

        viewModel.estado.test {
            skipItems(1)

            viewModel.onContinuar()
            val enCurso = awaitItem()
            assertTrue(
                "esperaba ProcesandoExtraccion, fue $enCurso",
                enCurso is CapturaUiState.ProcesandoExtraccion,
            )
            textEngine.gate?.complete(Unit)

            // Escenario 4: el motivo REAL del fallo, sin datos inventados ni
            // fallback a otro motor (ADR 0001).
            val fallo = awaitItem() as CapturaUiState.ExtraccionFallida
            assertEquals("Firebase AI no configurado", fallo.mensaje)
            assertEquals("fotos/captura_test.jpg", fallo.rutaFoto)

            // "Reintentar" vuelve a invocar el MISMO motor sobre el MISMO texto.
            textEngine.error = null
            viewModel.onReintentarExtraccion()
            val trasReintentar = awaitHastaQue("ExtraccionLista") {
                it is CapturaUiState.ExtraccionLista
            }
            assertTrue(trasReintentar is CapturaUiState.ExtraccionLista)
        }

        assertEquals(listOf("MERCADONA\nTOTAL 12,34", "MERCADONA\nTOTAL 12,34"), textEngine.received)
    }

    @Test
    fun textoBlankDevuelveResultadoRapidoSinInvocarElMotor() = runTest {
        engine.result = "" // OCR vacio: la foto no es legible (escenario 5)
        viewModel.onPermisoCambiado(true)
        viewModel.onImageCapturado(byteArrayOf(1), crop)
        assertEquals("", esperarRevision().textoCrudo)

        viewModel.estado.test {
            skipItems(1)

            viewModel.onContinuar()

            // ExtraerTicket NO invoca al motor con texto blank y el procesado
            // termina rapido en el resultado con "0/5 campos extraidos" (el
            // estado intermedio puede no llegar a verse por conflation).
            val lista = awaitHastaQue("ExtraccionLista") {
                it is CapturaUiState.ExtraccionLista
            } as CapturaUiState.ExtraccionLista
            assertEquals("", lista.extraido.comercio)
            assertEquals(null, lista.extraido.fechaEmision)
            assertEquals(null, lista.extraido.total)
            assertTrue(lista.extraido.lineas.isEmpty())
            assertEquals(Categoria.OTROS, lista.extraido.categoria)
            assertEquals("0/5 campos extra\u00eddos", etiquetaCompletitud(lista.extraido))
            expectNoEvents()
        }

        assertTrue("el motor no debe invocarse con texto blank", textEngine.received.isEmpty())
        // La foto NO queda "gastada": el Borrador sigue vivo y reintentable.
        assertTrue(store.deletedPaths.isEmpty())
    }

    @Test
    fun elBadgeExponeElMotorRealDelMotorDeExtraccion() = runTest {
        // Passthrough de ExtraerTicket.motor / TextEngine.motor (T1): el badge
        // muestra el motor ANTES de que termine la extraccion.
        assertEquals(Motor.NUBE, viewModel.motor)

        val local = EscanearViewModel(
            guardarFotoCapturada = GuardarFotoCapturada(processor, store),
            reconocerTextoCrudo = ReconocerTextoCrudo(store, engine),
            extraerTicket = ExtraerTicket(FakeTextEngine(motor = Motor.LOCAL)),
            photoFileStore = store,
        )
        assertEquals(Motor.LOCAL, local.motor)
    }

    /**
     * Espera con Turbine hasta el estado indicado, saltandose los intermedios
     * (p. ej. `ProcesandoExtraccion` cuando la extraccion termina rapido y el
     * estado intermedio puede llegar o no por conflation).
     */
    private suspend fun ReceiveTurbine<CapturaUiState>.awaitHastaQue(
        descripcion: String,
        esperado: (CapturaUiState) -> Boolean,
    ): CapturaUiState {
        repeat(6) {
            val item = awaitItem()
            if (esperado(item)) return item
        }
        error("No se alcanzo $descripcion")
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
