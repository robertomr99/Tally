package com.robertomr.tally.feature.escanear

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.robertomr.tally.domain.expenses.data.CropRegion
import com.robertomr.tally.domain.expenses.data.EngineRouter
import com.robertomr.tally.domain.expenses.data.TicketRepository
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.Linea
import com.robertomr.tally.domain.expenses.entities.LineaEditable
import com.robertomr.tally.domain.expenses.entities.DisponibilidadMotor
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.PreferenciaMotor
import com.robertomr.tally.domain.expenses.entities.Ticket
import com.robertomr.tally.domain.expenses.entities.TicketExtraido
import com.robertomr.tally.domain.expenses.entities.totalRecalculado
import com.robertomr.tally.domain.expenses.usecases.ExtraerTicket
import com.robertomr.tally.domain.expenses.usecases.ExtraerTicketConMotor
import com.robertomr.tally.domain.expenses.usecases.ObtenerOtroMotor
import com.robertomr.tally.domain.expenses.usecases.GuardarTicket
import com.robertomr.tally.domain.expenses.usecases.GuardarFotoCapturada
import com.robertomr.tally.domain.expenses.usecases.ReconocerTextoCrudo
import com.robertomr.tally.test.unit.CoroutineTestRule
import com.robertomr.tally.test.unit.FakeCapacidadDispositivo
import com.robertomr.tally.test.unit.FakeModelDownloader
import com.robertomr.tally.test.unit.FakeOcrEngine
import com.robertomr.tally.test.unit.FakePhotoFileStore
import com.robertomr.tally.test.unit.FakePhotoProcessor
import com.robertomr.tally.test.unit.FakeTextEngine
import com.robertomr.tally.test.unit.FakeTicketDataSource
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
 * badge con el motor real. Spec revision-datos: revision y guardado. Spec
 * edicion-lineas: anadir/editar/eliminar lineas, total intacto, entradas
 * invalidas ignoradas y persistencia de las lineas editadas.
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
    private val ticketDataSource = FakeTicketDataSource()
    private val repository = TicketRepository(ticketDataSource, store)
    private val localEngine = FakeTextEngine(motor = Motor.LOCAL)
    private val capacidad = FakeCapacidadDispositivo(capaz = true)
    private val downloader = FakeModelDownloader(ruta = null)
    private var pref = PreferenciaMotor.AUTO
    private val router = EngineRouter(localEngine, textEngine, capacidad, downloader) {
        pref
    }
    private val viewModel = crearViewModel(router)

    private fun crearViewModel(motorRouter: EngineRouter): EscanearViewModel = EscanearViewModel(
        guardarFotoCapturada = GuardarFotoCapturada(processor, store),
        reconocerTextoCrudo = ReconocerTextoCrudo(store, engine),
        extraerTicket = ExtraerTicket(motorRouter),
        extraerTicketConMotor = ExtraerTicketConMotor(motorRouter),
        obtenerOtroMotor = ObtenerOtroMotor(motorRouter),
        guardarTicket = GuardarTicket(repository),
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

    // --- Galeria (spec galeria-escaneo): misma ruta que la captura ---

    @Test
    fun imagenElegidaRecorreElMismoFlujoConLaImagenCompleta() = runTest {
        val elegida = byteArrayOf(4, 5, 6)
        processor.gate = CompletableDeferred()
        viewModel.onPermisoCambiado(true)

        viewModel.estado.test {
            assertEquals(CapturaUiState.Encuadre, awaitItem())

            viewModel.onImagenElegida(elegida)
            assertEquals(CapturaUiState.Procesando, awaitItem())

            processor.gate?.complete(Unit)
            val revision = awaitItem() as CapturaUiState.Revision
            assertEquals("fotos/captura_test.jpg", revision.rutaFoto)
            assertEquals("MERCADONA\nTOTAL 12,34", revision.textoCrudo)
        }

        // Sin recorte: el procesador recibe el centinela de imagen completa.
        assertEquals(listOf(CropRegion.Completo), processor.receivedCrops)
        assertTrue(processor.received[0].contentEquals(elegida))
        assertTrue(store.savedPayloads[0].contentEquals(byteArrayOf(9, 9, 9)))
    }

    @Test
    fun imagenElegidaSinPermisoDeCamaraLlegaARevisionYRepetirVuelveAlPermiso() = runTest {
        processor.gate = CompletableDeferred()

        viewModel.estado.test {
            assertEquals(CapturaUiState.PermisoPendiente, awaitItem())

            viewModel.onImagenElegida(byteArrayOf(1))
            assertEquals(CapturaUiState.Procesando, awaitItem())

            processor.gate?.complete(Unit)
            assertTrue(awaitItem() is CapturaUiState.Revision)

            // Repetir foto descarta la imagen y, sin camara, vuelve al permiso.
            viewModel.onRepetirFoto()
            assertEquals(CapturaUiState.PermisoPendiente, awaitItem())
        }
    }

    @Test
    fun imagenIlegibleSinPermisoMuestraErrorYVolverRegresaAlPermiso() = runTest {
        viewModel.estado.test {
            assertEquals(CapturaUiState.PermisoPendiente, awaitItem())

            viewModel.onImagenFallida("No se pudo leer la imagen. Prueba con otra foto.")
            assertEquals(
                CapturaUiState.Error("No se pudo leer la imagen. Prueba con otra foto."),
                awaitItem(),
            )
            assertTrue(processor.received.isEmpty())
            assertTrue(store.savedPayloads.isEmpty())

            viewModel.onRepetirFoto()
            assertEquals(CapturaUiState.PermisoPendiente, awaitItem())
        }
    }

    @Test
    fun imagenElegidaConOcrFallidoNoBloqueaLaRevision() = runTest {
        engine.error = IllegalStateException("motor OCR caido")
        processor.gate = CompletableDeferred()
        viewModel.onPermisoCambiado(true)

        viewModel.estado.test {
            assertEquals(CapturaUiState.Encuadre, awaitItem())

            viewModel.onImagenElegida(byteArrayOf(1))
            assertEquals(CapturaUiState.Procesando, awaitItem())

            processor.gate?.complete(Unit)
            val revision = awaitItem() as CapturaUiState.Revision
            assertEquals(true, revision.ocrNoDisponible)
        }
    }

    @Test
    fun imagenQueNoSePuedeProcesarMuestraElMensajeDelProcesador() = runTest {
        processor.error = IllegalStateException("La imagen no es válida")
        processor.gate = CompletableDeferred()
        viewModel.onPermisoCambiado(true)

        viewModel.estado.test {
            assertEquals(CapturaUiState.Encuadre, awaitItem())

            viewModel.onImagenElegida(byteArrayOf(1))
            assertEquals(CapturaUiState.Procesando, awaitItem())

            processor.gate?.complete(Unit)
            assertEquals(CapturaUiState.Error("La imagen no es válida"), awaitItem())
            assertTrue(store.savedPayloads.isEmpty())
        }
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

        downloader.ruta = "/m/model.task" // Auto con capaz + modelo -> Local

        assertEquals(Motor.LOCAL, viewModel.motor)
    }

    // --- Reintento con el otro motor (spec reintento-otro-motor) ---

    private val modeloLocal = "/m/model.litertlm"

    /** Entorno: Local disponible y la preferencia [p] fija el motor del primer intento. */
    private fun entornoConLocalDisponible(p: PreferenciaMotor) {
        capacidad.capaz = true
        downloader.ruta = modeloLocal
        pref = p
    }

    private fun llegarAlResultado(): CapturaUiState.ExtraccionLista {
        viewModel.onPermisoCambiado(true)
        viewModel.onImageCapturado(byteArrayOf(1), crop)
        esperarRevision()
        viewModel.onContinuar()
        return esperarEstado { it is CapturaUiState.ExtraccionLista } as CapturaUiState.ExtraccionLista
    }

    @Test
    fun reintentoDesdeElResultadoUsaElOtroMotorSobreElMismoTextoSinOcrNiFoto() = runTest {
        entornoConLocalDisponible(PreferenciaMotor.AUTO) // primer intento: Local
        val r1 = llegarAlResultado()
        assertEquals(Motor.LOCAL, r1.extraido.motor)
        assertEquals(Motor.NUBE, r1.otro.motor)
        assertEquals(DisponibilidadMotor.DISPONIBLE, r1.otro.disponibilidad)
        val ocrAntes = engine.received.size
        val fotosAntes = store.savedPayloads.size

        textEngine.gate = CompletableDeferred()
        viewModel.onReintentarConOtroMotor()

        // Badge honesto: el estado lleva el motor del NUEVO intento y R1 queda
        // conservado mientras dura el reintento (D4).
        val procesando = esperarEstado { it is CapturaUiState.ProcesandoExtraccion } as CapturaUiState.ProcesandoExtraccion
        assertEquals(Motor.NUBE, procesando.motor)
        assertEquals(r1.extraido, procesando.anterior)

        textEngine.gate?.complete(Unit)
        val r2 = esperarEstado { it is CapturaUiState.ExtraccionLista } as CapturaUiState.ExtraccionLista
        assertEquals(Motor.NUBE, r2.extraido.motor)
        assertEquals(Motor.LOCAL, r2.otro.motor)

        // Mismo texto crudo del Borrador, sin OCR ni foto nuevos.
        assertEquals(listOf("MERCADONA\nTOTAL 12,34"), textEngine.received)
        assertEquals(listOf("MERCADONA\nTOTAL 12,34"), localEngine.received)
        assertEquals(ocrAntes, engine.received.size)
        assertEquals(fotosAntes, store.savedPayloads.size)
    }

    @Test
    fun cancelarElReintentoDesdeElResultadoRestauraElResultadoAnterior() = runTest {
        entornoConLocalDisponible(PreferenciaMotor.NUBE) // primer intento: Nube
        val r1 = llegarAlResultado()
        assertEquals(Motor.NUBE, r1.extraido.motor)

        localEngine.gate = CompletableDeferred()
        viewModel.onReintentarConOtroMotor()
        val procesando = esperarEstado { it is CapturaUiState.ProcesandoExtraccion } as CapturaUiState.ProcesandoExtraccion
        assertEquals(Motor.LOCAL, procesando.motor)
        assertEquals(1, localEngine.received.size)

        // Cancelar (o back) vuelve a R1 intacto, no a la pantalla 05, y la
        // cancelacion nunca se convierte en error.
        viewModel.onCancelarProcesado()
        assertEquals(r1, esperarEstado { it is CapturaUiState.ExtraccionLista })
    }

    @Test
    fun elFalloDelReintenteConservaElAnteriorYPermiteRestaurarlo() = runTest {
        entornoConLocalDisponible(PreferenciaMotor.NUBE)
        val r1 = llegarAlResultado()

        localEngine.error = IllegalStateException("modelo roto")
        viewModel.onReintentarConOtroMotor()
        val fallo = esperarEstado { it is CapturaUiState.ExtraccionFallida } as CapturaUiState.ExtraccionFallida
        assertEquals("modelo roto", fallo.mensaje)
        assertEquals(Motor.LOCAL, fallo.motor) // badge FALLIDO del motor nuevo
        assertEquals(r1.extraido, fallo.anterior)
        assertEquals(Motor.NUBE, fallo.otro.motor)

        viewModel.onVolverAlResultadoAnterior()
        assertEquals(r1, esperarEstado { it is CapturaUiState.ExtraccionLista })
    }

    @Test
    fun volverALaFotoDesdeElErrorDeUnReintenteDescartaElAnterior() = runTest {
        entornoConLocalDisponible(PreferenciaMotor.NUBE)
        llegarAlResultado()
        localEngine.error = IllegalStateException("x")
        viewModel.onReintentarConOtroMotor()
        esperarEstado { it is CapturaUiState.ExtraccionFallida }

        viewModel.onVolverALaFoto()

        esperarRevision()
        assertTrue(store.deletedPaths.isEmpty())
    }

    @Test
    fun reintentarConElMismoMotorUsaElQueFalloNoElDelRouterActual() = runTest {
        entornoConLocalDisponible(PreferenciaMotor.NUBE)
        textEngine.error = IllegalStateException("sin cuota")
        viewModel.onPermisoCambiado(true)
        viewModel.onImageCapturado(byteArrayOf(1), crop)
        esperarRevision()
        viewModel.onContinuar()
        val fallo = esperarEstado { it is CapturaUiState.ExtraccionFallida } as CapturaUiState.ExtraccionFallida
        assertEquals(Motor.NUBE, fallo.motor)
        assertNull(fallo.anterior)

        // El entorno cambia: la politica Auto elegiria Local, pero "Reintentar"
        // repite con el motor que fallo (Nube).
        pref = PreferenciaMotor.AUTO
        textEngine.error = null
        viewModel.onReintentarExtraccion()

        val lista = esperarEstado { it is CapturaUiState.ExtraccionLista } as CapturaUiState.ExtraccionLista
        assertEquals(Motor.NUBE, lista.extraido.motor)
        assertEquals(2, textEngine.received.size)
        assertTrue(localEngine.received.isEmpty())
    }

    @Test
    fun probarConElOtroMotorDesdeElErrorYCancelarVuelveALaPantalla05() = runTest {
        entornoConLocalDisponible(PreferenciaMotor.NUBE)
        textEngine.error = IllegalStateException("sin red")
        viewModel.onPermisoCambiado(true)
        viewModel.onImageCapturado(byteArrayOf(1), crop)
        val revision = esperarRevision()
        viewModel.onContinuar()
        val fallo = esperarEstado { it is CapturaUiState.ExtraccionFallida } as CapturaUiState.ExtraccionFallida
        assertEquals(DisponibilidadMotor.DISPONIBLE, fallo.otro.disponibilidad)

        localEngine.gate = CompletableDeferred()
        viewModel.onReintentarConOtroMotor()
        val procesando = esperarEstado { it is CapturaUiState.ProcesandoExtraccion } as CapturaUiState.ProcesandoExtraccion
        assertEquals(Motor.LOCAL, procesando.motor)
        assertNull(procesando.anterior)
        assertEquals(listOf("MERCADONA\nTOTAL 12,34"), localEngine.received)

        viewModel.onCancelarProcesado()
        assertEquals(revision, esperarRevision())
    }

    @Test
    fun conElOtroMotorNoDisponibleElEventoSeIgnoraYLaOfertaLlevaElMotivo() = runTest {
        pref = PreferenciaMotor.NUBE
        capacidad.capaz = true
        downloader.ruta = null
        val sinModelo = llegarAlResultado()
        assertEquals(DisponibilidadMotor.SIN_MODELO, sinModelo.otro.disponibilidad)
        assertEquals(
            "Descarga el modelo local en Ajustes para usarlo",
            motivoNoDisponible(sinModelo.otro.disponibilidad),
        )

        viewModel.onReintentarConOtroMotor()

        assertEquals(sinModelo, viewModel.estado.value)
        assertTrue(localEngine.received.isEmpty())
        assertEquals(
            "Este dispositivo no puede ejecutar el motor local",
            motivoNoDisponible(DisponibilidadMotor.NO_CAPAZ),
        )
        assertNull(motivoNoDisponible(DisponibilidadMotor.DISPONIBLE))
    }

    @Test
    fun siElModeloDesapareceTrasMostrarElBotonElErrorExplicaElMotivoSinInvocarElMotor() = runTest {
        entornoConLocalDisponible(PreferenciaMotor.NUBE)
        val r1 = llegarAlResultado()
        downloader.ruta = null // el usuario elimina el modelo con el resultado en pantalla

        viewModel.onReintentarConOtroMotor()

        val fallo = esperarEstado { it is CapturaUiState.ExtraccionFallida } as CapturaUiState.ExtraccionFallida
        assertEquals("Descarga el modelo local en Ajustes para usarlo", fallo.mensaje)
        assertEquals(Motor.LOCAL, fallo.motor)
        assertEquals(r1.extraido, fallo.anterior)
        assertTrue(localEngine.received.isEmpty())
    }

    @Test
    fun dobleToqueDelBotonCuentaComoUnSoloIntento() = runTest {
        entornoConLocalDisponible(PreferenciaMotor.AUTO)
        llegarAlResultado()
        textEngine.gate = CompletableDeferred()

        viewModel.onReintentarConOtroMotor()
        viewModel.onReintentarConOtroMotor()
        textEngine.gate?.complete(Unit)

        esperarEstado { it is CapturaUiState.ExtraccionLista && it.extraido.motor == Motor.NUBE }
        assertEquals(1, textEngine.received.size)
    }

    @Test
    fun guardarTrasElReintentoPersisteElMotorDelResultadoAceptado() = runTest {
        entornoConLocalDisponible(PreferenciaMotor.AUTO) // R1 Local
        llegarAlResultado()
        viewModel.onReintentarConOtroMotor() // R2 Nube
        esperarEstado { it is CapturaUiState.ExtraccionLista && it.extraido.motor == Motor.NUBE }

        viewModel.onRevisar()
        esperarEstado { it is CapturaUiState.RevisionDatos }
        viewModel.onGuardar()
        esperarEstado { it is CapturaUiState.Guardado }

        val ticket = ticketsGuardados().single()
        assertEquals(Motor.NUBE, ticket.motor)
        assertEquals(35.30, ticket.total, 0.001)
    }

    @Test
    fun elBadgeDelProcesadoSaleDelEstadoNoDelRouterRecomputado() = runTest {
        pref = PreferenciaMotor.AUTO
        downloader.ruta = null // Continuar resuelve Nube
        viewModel.onPermisoCambiado(true)
        viewModel.onImageCapturado(byteArrayOf(1), crop)
        esperarRevision()
        textEngine.gate = CompletableDeferred()
        viewModel.onContinuar()
        val procesando = esperarEstado { it is CapturaUiState.ProcesandoExtraccion } as CapturaUiState.ProcesandoExtraccion
        assertEquals(Motor.NUBE, procesando.motor)

        downloader.ruta = modeloLocal // el entorno cambia en pleno procesado
        assertEquals(Motor.LOCAL, viewModel.motor)
        assertEquals(Motor.NUBE, (esperarEstado { it is CapturaUiState.ProcesandoExtraccion } as CapturaUiState.ProcesandoExtraccion).motor)
    }

    // --- Revision y guardado (spec revision-datos) ---

    @Test
    fun revisarEditaLaCabeceraYGuardarPersisteElTicketEnRoom() = runTest {
        viewModel.onPermisoCambiado(true)
        viewModel.onImageCapturado(byteArrayOf(1), crop)
        esperarRevision()

        viewModel.estado.test {
            skipItems(1) // Revision (pantalla 05)

            viewModel.onContinuar()
            awaitHastaQue("ExtraccionLista") { it is CapturaUiState.ExtraccionLista }

            // "Revisar ticket" abre la revision editable con la cabecera
            // prefijada de la extraccion.
            viewModel.onRevisar()
            val revision = awaitHastaQue("RevisionDatos") {
                it is CapturaUiState.RevisionDatos
            } as CapturaUiState.RevisionDatos
            assertEquals(textEngine.result, revision.extraido)

            viewModel.onComercioEditado("TIENDA X")
            awaitHastaQue("comercio editado") {
                it is CapturaUiState.RevisionDatos && it.datos.comercio == "TIENDA X"
            }
            viewModel.onCategoriaElegida(Categoria.HOGAR)
            val conCategoria = awaitHastaQue("categoria HOGAR") {
                it is CapturaUiState.RevisionDatos && it.datos.categoria == Categoria.HOGAR
            } as CapturaUiState.RevisionDatos
            assertEquals(Categoria.HOGAR, conCategoria.datos.categoria)

            viewModel.onGuardar()
            awaitHastaQue("Guardado") { it is CapturaUiState.Guardado }
        }

        // Se persiste UN ticket con la cabecera corregida y el resto del Borrador.
        val ticket = ticketsGuardados().single()
        assertEquals("TIENDA X", ticket.comercio)
        assertEquals(Categoria.HOGAR, ticket.categoria)
        assertEquals(35.30, ticket.total, 0.0)
        assertEquals(textEngine.result?.motor, ticket.motor)
        assertEquals("fotos/captura_test.jpg", ticket.rutaFoto)
        assertEquals("MERCADONA\nTOTAL 12,34", ticket.textoCrudoOcr)
        assertTrue(ticket.lineas.isNotEmpty())
    }

    @Test
    fun guardarSinRellenarNadaPersisteConLosDefaults() = runTest {
        engine.result = "" // OCR vacio (foto ilegible): extraccion vacia
        viewModel.onPermisoCambiado(true)
        viewModel.onImageCapturado(byteArrayOf(1), crop)
        assertEquals("", esperarRevision().textoCrudo)
        val antes = LocalDateTime.now()

        viewModel.estado.test {
            skipItems(1)

            viewModel.onContinuar()
            awaitHastaQue("ExtraccionLista") { it is CapturaUiState.ExtraccionLista }

            viewModel.onRevisar()
            val revision = awaitHastaQue("RevisionDatos") {
                it is CapturaUiState.RevisionDatos
            } as CapturaUiState.RevisionDatos
            // Escenario 2: defaults sin bloquear (comercio "", fecha hoy,
            // total 0.0, categoria OTROS).
            assertEquals("", revision.datos.comercio)
            assertEquals(0.0, revision.datos.total, 0.0)
            assertEquals(Categoria.OTROS, revision.datos.categoria)
            assertTrue(!revision.datos.fechaEmision.isBefore(antes))

            viewModel.onGuardar()
            awaitHastaQue("Guardado") { it is CapturaUiState.Guardado }
        }

        val ticket = ticketsGuardados().single()
        assertEquals("", ticket.comercio)
        assertEquals(0.0, ticket.total, 0.0)
        assertEquals(Categoria.OTROS, ticket.categoria)
        assertTrue(ticket.lineas.isEmpty())
        assertEquals(Motor.NUBE, ticket.motor)
        assertEquals("fotos/captura_test.jpg", ticket.rutaFoto)
    }

    @Test
    fun volverAlResultadoSinGuardarNoEscribeEnRoom() = runTest {
        viewModel.onPermisoCambiado(true)
        viewModel.onImageCapturado(byteArrayOf(1), crop)
        esperarRevision()

        viewModel.estado.test {
            skipItems(1)

            viewModel.onContinuar()
            awaitHastaQue("ExtraccionLista") { it is CapturaUiState.ExtraccionLista }

            viewModel.onRevisar()
            awaitHastaQue("RevisionDatos") { it is CapturaUiState.RevisionDatos }

            // Escenario 6: back (o "Volver") no escribe nada y vuelve al
            // resultado con el Borrador intacto.
            viewModel.onVolverAlResultado()
            val deVuelta = awaitHastaQue("ExtraccionLista de vuelta") {
                it is CapturaUiState.ExtraccionLista
            }
            assertTrue(deVuelta is CapturaUiState.ExtraccionLista)
        }

        assertTrue("nada debe persistirse al volver", ticketsGuardados().isEmpty())
        assertTrue("la foto no se borra al volver", store.deletedPaths.isEmpty())
    }

    @Test
    fun trasGuardarElSubflujoSeReseteaAEncuadre() = runTest {
        viewModel.onPermisoCambiado(true)
        viewModel.onImageCapturado(byteArrayOf(1), crop)
        esperarRevision()
        viewModel.onContinuar()
        esperarEstado { it is CapturaUiState.ExtraccionLista }
        viewModel.onRevisar()
        esperarEstado { it is CapturaUiState.RevisionDatos }

        viewModel.onGuardar()
        esperarEstado { it is CapturaUiState.Guardado }

        // La pantalla emite el evento y resetea el subflujo (R-REV3): volver a
        // Escanear debe mostrar la camara limpia, no la revision del guardado.
        viewModel.onGuardadoVisto()

        assertEquals(CapturaUiState.Encuadre, esperarEstado { it is CapturaUiState.Encuadre })
    }

    @Test
    fun guardarFallidoVuelveALaRevisionConElMotivoReal() = runTest {
        ticketDataSource.saveError = IllegalStateException("disco lleno")
        viewModel.onPermisoCambiado(true)
        viewModel.onImageCapturado(byteArrayOf(1), crop)
        esperarRevision()

        viewModel.estado.test {
            skipItems(1)

            viewModel.onContinuar()
            awaitHastaQue("ExtraccionLista") { it is CapturaUiState.ExtraccionLista }
            viewModel.onRevisar()
            awaitHastaQue("RevisionDatos") { it is CapturaUiState.RevisionDatos }

            viewModel.onGuardar()
            val fallo = awaitHastaQue("RevisionDatos con error") {
                it is CapturaUiState.RevisionDatos && it.errorGuardado != null
            } as CapturaUiState.RevisionDatos
            assertEquals("disco lleno", fallo.errorGuardado)
        }

        assertTrue(ticketsGuardados().isEmpty())
    }

    @Test
    fun reintentarTrasFalloGuardaSinEditarNadaPersisteElTicket() = runTest {
        ticketDataSource.saveError = IllegalStateException("disco lleno")
        viewModel.onPermisoCambiado(true)
        viewModel.onImageCapturado(byteArrayOf(1), crop)
        esperarRevision()

        viewModel.estado.test {
            skipItems(1)

            viewModel.onContinuar()
            awaitHastaQue("ExtraccionLista") { it is CapturaUiState.ExtraccionLista }
            viewModel.onRevisar()
            awaitHastaQue("RevisionDatos") { it is CapturaUiState.RevisionDatos }

            viewModel.onGuardar()
            awaitHastaQue("fallo de guardado") {
                it is CapturaUiState.RevisionDatos && it.errorGuardado != null
            }

            // Se arregla el problema y se reintenta SIN editar ningun campo: el
            // segundo "Guardar" debe re-dispararse (no un no-op silencioso).
            ticketDataSource.saveError = null
            viewModel.onGuardar()
            awaitHastaQue("Guardado en el reintento") { it is CapturaUiState.Guardado }
        }

        assertEquals(1, ticketsGuardados().size)
    }

    @Test
    fun editarFechaYTotalActualizaLaCabeceraEIgnoraTextoInvalido() = runTest {
        viewModel.onPermisoCambiado(true)
        viewModel.onImageCapturado(byteArrayOf(1), crop)
        esperarRevision()
        viewModel.onContinuar()
        esperarEstado { it is CapturaUiState.ExtraccionLista }
        viewModel.onRevisar()
        esperarEstado { it is CapturaUiState.RevisionDatos }

        viewModel.onFechaEditada("3/10/2026")
        viewModel.onTotalEditado("12,34")
        val editada = esperarEstado {
            it is CapturaUiState.RevisionDatos && it.datos.total == 12.34
        } as CapturaUiState.RevisionDatos
        assertEquals(LocalDate.of(2026, 10, 3), editada.datos.fechaEmision.toLocalDate())

        // Texto no parseable: se ignora y la cabecera no cambia.
        viewModel.onFechaEditada("no es fecha")
        viewModel.onTotalEditado("abc")
        val igual = esperarEstado { it is CapturaUiState.RevisionDatos } as CapturaUiState.RevisionDatos
        assertEquals(12.34, igual.datos.total, 0.0)
        assertEquals(LocalDate.of(2026, 10, 3), igual.datos.fechaEmision.toLocalDate())
    }

    // --- Edicion de lineas (spec edicion-lineas) ---

    @Test
    fun revisarPrefijaLasLineasExtraidasComoEditables() = runTest {
        val revision = abrirRevisionDatos()

        assertEquals(
            listOf(LineaEditable(0, "LECHE", 2, 1.05)),
            revision.datos.lineas,
        )
    }

    @Test
    fun anadirEditarYEliminarLineasPersisteExactamenteLasEditadas() = runTest {
        // Escenario 1: se parte de LECHE (id 0); se anaden dos filas, se edita
        // una, se elimina LECHE y se guarda.
        abrirRevisionDatos()

        viewModel.onLineaAnadida() // id 1
        viewModel.onLineaAnadida() // id 2
        viewModel.onLineaConceptoEditado(1, "PAN")
        viewModel.onLineaCantidadEditada(1, "3")
        viewModel.onLineaPrecioEditado(1, "0,90")
        viewModel.onLineaConceptoEditado(2, "CAFE")
        viewModel.onLineaPrecioEditado(2, "1.5")
        viewModel.onLineaEliminada(0)

        val editada = esperarEstado {
            it is CapturaUiState.RevisionDatos && it.datos.lineas.size == 2 &&
                it.datos.lineas.none { linea -> linea.id == 0L }
        } as CapturaUiState.RevisionDatos
        assertEquals(
            listOf(
                LineaEditable(1, "PAN", 3, 0.90),
                LineaEditable(2, "CAFE", 1, 1.5),
            ),
            editada.datos.lineas,
        )

        viewModel.onGuardar()
        esperarEstado { it is CapturaUiState.Guardado }

        val ticket = ticketsGuardados().single()
        assertEquals(listOf(Linea("PAN", 3, 0.90), Linea("CAFE", 1, 1.5)), ticket.lineas)
    }

    @Test
    fun editarLineasNoCambiaElTotalDelTicketNiElPersistido() = runTest {
        // Escenario 2: total extraido 35.30; las lineas suman otra cosa.
        abrirRevisionDatos()

        viewModel.onLineaCantidadEditada(0, "10")
        viewModel.onLineaAnadida()
        viewModel.onLineaConceptoEditado(1, "EXTRA")
        viewModel.onLineaPrecioEditado(1, "4,00")
        val editada = esperarEstado {
            it is CapturaUiState.RevisionDatos && it.datos.lineas.size == 2 &&
                it.datos.lineas[1].precioUnitario == 4.0
        } as CapturaUiState.RevisionDatos

        assertEquals(10 * 1.05 + 4.0, totalRecalculado(editada.datos.lineas), 1e-9)
        assertEquals(35.30, editada.datos.total, 0.0)

        viewModel.onGuardar()
        esperarEstado { it is CapturaUiState.Guardado }
        assertEquals(35.30, ticketsGuardados().single().total, 0.0)
    }

    @Test
    fun editarElTotalExplicitamenteSiCambiaElTotalPersistido() = runTest {
        // Escenario 2 (contraparte): solo la edicion explicita del total lo cambia.
        abrirRevisionDatos()
        viewModel.onLineaEliminada(0)
        viewModel.onTotalEditado("20,00")
        esperarEstado { it is CapturaUiState.RevisionDatos && it.datos.total == 20.0 }

        viewModel.onGuardar()
        esperarEstado { it is CapturaUiState.Guardado }

        val ticket = ticketsGuardados().single()
        assertEquals(20.0, ticket.total, 0.0)
        assertTrue(ticket.lineas.isEmpty())
    }

    @Test
    fun cantidadOPrecioInvalidosSeIgnoranSinRomperLaRevision() = runTest {
        // Escenario 4: "abc", "0", "-3", "2,5" y vacio en cantidad; "abc", "-1" y
        // "NaN" en precio (el precio vacio es 0, ver precioVacioCuentaComoCero).
        abrirRevisionDatos()

        listOf("abc", "0", "-3", "2,5", "").forEach {
            viewModel.onLineaCantidadEditada(0, it)
        }
        listOf("abc", "-1", "NaN").forEach {
            viewModel.onLineaPrecioEditado(0, it)
        }
        // Una edicion a una fila inexistente tampoco rompe nada.
        viewModel.onLineaConceptoEditado(99, "FANTASMA")
        viewModel.onLineaEliminada(99)

        val actual = esperarEstado { it is CapturaUiState.RevisionDatos } as CapturaUiState.RevisionDatos
        assertEquals(listOf(LineaEditable(0, "LECHE", 2, 1.05)), actual.datos.lineas)

        // "Guardar cambios" sigue disponible y persiste.
        viewModel.onGuardar()
        esperarEstado { it is CapturaUiState.Guardado }
        assertEquals(listOf(Linea("LECHE", 2, 1.05)), ticketsGuardados().single().lineas)
    }

    @Test
    fun precioVacioCuentaComoCeroYElTotalRecalculadoLoRefleja() = runTest {
        // L1 (validator edicion-lineas): vaciar el precio no deja el valor anterior.
        abrirRevisionDatos()

        viewModel.onLineaPrecioEditado(0, "")
        val editada = esperarEstado {
            it is CapturaUiState.RevisionDatos && it.datos.lineas.single().precioUnitario == 0.0
        } as CapturaUiState.RevisionDatos

        assertEquals(0.0, totalRecalculado(editada.datos.lineas), 0.0)
        assertEquals(35.30, editada.datos.total, 0.0)
    }

    @Test
    fun anadirTrasBorrarLaUltimaLineaNoReutilizaSuId() = runTest {
        // L2 (validator edicion-lineas): ids que nunca se reutilizan.
        abrirRevisionDatos()

        viewModel.onLineaAnadida() // id 1
        viewModel.onLineaEliminada(1)
        viewModel.onLineaAnadida() // id 2, no 1
        val actual = esperarEstado {
            it is CapturaUiState.RevisionDatos && it.datos.lineas.size == 2
        } as CapturaUiState.RevisionDatos

        assertEquals(listOf(0L, 2L), actual.datos.lineas.map { it.id })
    }

    @Test
    fun extraccionVaciaAnadirLineasAManoYGuardarPersisteLasIntroducidas() = runTest {
        // Escenario 5: OCR vacio -> extraccion sin lineas -> se anaden a mano.
        engine.result = ""
        val revision = abrirRevisionDatos()
        assertTrue(revision.datos.lineas.isEmpty())

        viewModel.onLineaAnadida() // id 0
        viewModel.onLineaConceptoEditado(0, "MENU DEL DIA")
        viewModel.onLineaPrecioEditado(0, "12,50")
        viewModel.onLineaAnadida() // id 1, se queda en blanco -> se descarta
        esperarEstado { it is CapturaUiState.RevisionDatos && it.datos.lineas.size == 2 }

        viewModel.onGuardar()
        esperarEstado { it is CapturaUiState.Guardado }

        val ticket = ticketsGuardados().single()
        assertEquals(listOf(Linea("MENU DEL DIA", 1, 12.50)), ticket.lineas)
        assertEquals(0.0, ticket.total, 0.0)
    }

    @Test
    fun descartarConLineasEditadasNoEscribeYRevisarDeNuevoPartiendoDeLaExtraccion() = runTest {
        // Escenario 6: "Descartar" (= back) no escribe; volver a revisar parte
        // otra vez de las lineas extraidas.
        abrirRevisionDatos()
        viewModel.onLineaEliminada(0)
        viewModel.onLineaAnadida()
        // Sin filas, la nueva va en blanco (su id no reutiliza el de LECHE).
        esperarEstado {
            it is CapturaUiState.RevisionDatos && it.datos.lineas.singleOrNull()?.concepto == ""
        }

        viewModel.onVolverAlResultado()
        esperarEstado { it is CapturaUiState.ExtraccionLista }
        assertTrue(ticketsGuardados().isEmpty())
        assertTrue(store.deletedPaths.isEmpty())

        viewModel.onRevisar()
        val otraVez = esperarEstado { it is CapturaUiState.RevisionDatos } as CapturaUiState.RevisionDatos
        assertEquals(listOf(LineaEditable(0, "LECHE", 2, 1.05)), otraVez.datos.lineas)
    }

    @Test
    fun editarUnaLineaTrasUnFalloDeGuardadoLimpiaElError() = runTest {
        ticketDataSource.saveError = IllegalStateException("disco lleno")
        abrirRevisionDatos()
        viewModel.onGuardar()
        esperarEstado { it is CapturaUiState.RevisionDatos && it.errorGuardado != null }

        viewModel.onLineaConceptoEditado(0, "LECHE ENTERA")
        val limpia = esperarEstado {
            it is CapturaUiState.RevisionDatos && it.errorGuardado == null
        } as CapturaUiState.RevisionDatos
        assertEquals("LECHE ENTERA", limpia.datos.lineas.single().concepto)
    }

    /** Captura -> Continuar -> Revisar ticket; devuelve la revision abierta. */
    private fun abrirRevisionDatos(): CapturaUiState.RevisionDatos {
        viewModel.onPermisoCambiado(true)
        viewModel.onImageCapturado(byteArrayOf(1), crop)
        esperarRevision()
        viewModel.onContinuar()
        esperarEstado { it is CapturaUiState.ExtraccionLista }
        viewModel.onRevisar()
        return esperarEstado { it is CapturaUiState.RevisionDatos } as CapturaUiState.RevisionDatos
    }

    /** Espera con Turbine hasta el estado indicado, saltandose los intermedios
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

    /** Tickets persistidos (via el repositorio, como los observara el listado). */
    private suspend fun ticketsGuardados(): List<Ticket> =
        repository.observeByDateRange(LocalDate.MIN, LocalDate.MAX).first()
}
