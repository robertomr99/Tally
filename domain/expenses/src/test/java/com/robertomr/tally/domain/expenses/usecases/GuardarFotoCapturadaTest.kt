package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.test.unit.CoroutineTestRule
import com.robertomr.tally.test.unit.FakePhotoFileStore
import com.robertomr.tally.test.unit.FakePhotoProcessor
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Tests JVM de [GuardarFotoCapturada] con los dobles de :test:unit (spec
 * captura-foto, T5): process -> save en orden, nombre relativo y errores del
 * processor propagados.
 */
class GuardarFotoCapturadaTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private val processor = FakePhotoProcessor(processedBytes = byteArrayOf(9, 8, 7))
    private val store = FakePhotoFileStore(savedName = "fotos/captura_test.jpg")
    private val useCase = GuardarFotoCapturada(processor, store)

    @Test
    fun processRunsBeforeSaveAndSavesProcessedBytes() = runTest {
        val raw = byteArrayOf(1, 2, 3)

        val ruta = useCase(raw)

        // process -> save, en ese orden y con los bytes procesados (no los crudos).
        assertEquals(listOf<ByteArray>(raw), processor.received)
        assertEquals(1, store.savedPayloads.size)
        assertTrue(processor.received[0].contentEquals(raw))
        assertTrue(store.savedPayloads[0].contentEquals(byteArrayOf(9, 8, 7)))
        assertEquals("fotos/captura_test.jpg", ruta)
    }

    @Test
    fun returnedNameIsRelativeNeverAbsolute() = runTest {
        val ruta = useCase(byteArrayOf(1))

        // Contrato de seguridad: nombre relativo bajo filesDir, nunca absoluto.
        assertFalse(File(ruta).isAbsolute)
        assertTrue(ruta.startsWith("fotos/"))
    }

    @Test
    fun processorErrorPropagatesAndNothingIsSaved() {
        processor.error = IllegalStateException("fallo del pipeline")

        val error = assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { useCase(byteArrayOf(1)) }
        }

        assertEquals("fallo del pipeline", error.message)
        assertTrue(store.savedPayloads.isEmpty())
    }

    @Test
    fun saveErrorPropagates() {
        store.saveError = IllegalStateException("disco lleno")

        val error = assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { useCase(byteArrayOf(1)) }
        }

        assertEquals("disco lleno", error.message)
    }
}
