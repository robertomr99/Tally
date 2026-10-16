package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.test.unit.FakeOcrEngine
import com.robertomr.tally.test.unit.FakePhotoFileStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests JVM de [ReconocerTextoCrudo] con los dobles de :test:unit (spec
 * ocr-mlkit, T4): read -> recognize en orden, texto del motor tal cual, foto
 * ausente -> "", error del motor propagado y CancellationException re-lanzada
 * (nunca convertida en resultado).
 */
class ReconocerTextoCrudoTest {

    private val store = FakePhotoFileStore()
    private val engine = FakeOcrEngine(result = "MERCADONA\nTOTAL 12,34")
    private val casoDeUso = ReconocerTextoCrudo(store, engine)

    @Test
    fun readHappensBeforeRecognizeAndFeedsItTheStoredBytes() = runBlocking {
        val jpeg = byteArrayOf(1, 2, 3, 4)
        store.readPayloads = mapOf("fotos/captura_1.jpg" to jpeg)

        val texto = casoDeUso("fotos/captura_1.jpg")

        // Orden read -> recognize: el motor recibe EXACTAMENTE los bytes que
        // devolvio la lectura (imposible si recognize corriera antes de read).
        assertEquals(listOf("fotos/captura_1.jpg"), store.readPaths)
        assertEquals(1, engine.received.size)
        assertArrayEquals(jpeg, engine.received[0])
        assertEquals("MERCADONA\nTOTAL 12,34", texto)
    }

    @Test
    fun missingPhotoFileReturnsEmptyTextWithoutTouchingTheEngine() = runBlocking {
        val texto = casoDeUso("fotos/no_existe.jpg")

        assertEquals("", texto)
        assertEquals(listOf("fotos/no_existe.jpg"), store.readPaths)
        assertTrue(engine.received.isEmpty())
    }

    @Test
    fun emptyOcrResultIsReturnedAsEmptyText() = runBlocking {
        store.readPayloads = mapOf("fotos/captura_2.jpg" to byteArrayOf(9))
        engine.result = ""

        val texto = casoDeUso("fotos/captura_2.jpg")

        assertEquals("", texto)
        assertEquals(1, engine.received.size)
    }

    @Test(expected = IllegalStateException::class)
    fun engineErrorPropagatesToTheCaller(): Unit = runBlocking {
        store.readPayloads = mapOf("fotos/captura_3.jpg" to byteArrayOf(9))
        engine.error = IllegalStateException("motor OCR caido")

        casoDeUso("fotos/captura_3.jpg")
    }

    @Test
    fun cancellationIsRethrownAndNeverConvertedIntoAResult() = runBlocking {
        store.readPayloads = mapOf("fotos/captura_4.jpg" to byteArrayOf(9))
        engine.gate = CompletableDeferred() // nunca se completa: retiene en recognize

        var capturada: Throwable? = null
        // En Dispatchers.Default: el cuerpo de runBlocking espera activamente
        // (Thread.sleep) y no debe bloquear el hilo del trabajo suspendido.
        val trabajo = launch(Dispatchers.Default) {
            try {
                casoDeUso("fotos/captura_4.jpg")
            } catch (e: Throwable) {
                capturada = e
                throw e
            }
        }
        // Deja que el caso de uso entre en recognize (retenido por el gate) y
        // cancela: la CancellationException debe salir tal cual, no como
        // Result.failure ni como texto "".
        val limite = System.currentTimeMillis() + 2_000
        while (engine.received.isEmpty() && System.currentTimeMillis() < limite) Thread.sleep(5)
        assertTrue("el motor no llego a invocarse", engine.received.isNotEmpty())
        trabajo.cancelAndJoin()

        assertTrue("esperaba CancellationException, fue $capturada", capturada is CancellationException)
    }
}
