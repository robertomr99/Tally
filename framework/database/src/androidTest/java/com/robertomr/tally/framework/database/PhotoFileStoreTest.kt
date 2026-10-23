package com.robertomr.tally.framework.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.robertomr.tally.framework.database.source.PhotoFileStoreImpl
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentados de [PhotoFileStoreImpl] con un fichero real en
 * `filesDir` (spec persistencia-room, escenario 2: borrado best-effort de la
 * foto del ticket; spec captura-foto: `save` con nombre relativo bajo
 * `filesDir/fotos/`).
 */
@RunWith(AndroidJUnit4::class)
class PhotoFileStoreTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val store = PhotoFileStoreImpl(context)
    private val testFile = File(context.filesDir, "photo-file-store-test.jpg")
    private val guardados = mutableListOf<String>()

    @Before
    fun setUp() {
        testFile.writeBytes(byteArrayOf(1, 2, 3))
    }

    @After
    fun tearDown() {
        testFile.delete()
        guardados.forEach { File(context.filesDir, it).delete() }
        File(context.filesDir, "fotos").delete()
    }

    @Test
    fun deleteRemovesExistingFileGivenRelativeName() {
        assertTrue(testFile.exists())

        val borrado = store.delete(testFile.name)

        assertTrue(borrado)
        assertFalse(testFile.exists())
    }

    @Test
    fun deleteRemovesExistingFileGivenAbsolutePath() {
        assertTrue(testFile.exists())

        val borrado = store.delete(testFile.absolutePath)

        assertTrue(borrado)
        assertFalse(testFile.exists())
    }

    @Test
    fun deleteReturnsFalseForMissingFileWithoutCrashing() {
        testFile.delete()

        assertFalse(store.delete(testFile.name))
    }

    @Test
    fun deleteReturnsFalseForBlankPath() {
        assertFalse(store.delete(""))
    }

    // --- save (spec captura-foto): nombre relativo bajo filesDir/fotos/ ---

    @Test
    fun saveWritesTheFileUnderFotosAndReturnsRelativeName() = runBlocking {
        val jpeg = byteArrayOf(10, 20, 30, 40)

        val ruta = store.save(jpeg)
        guardados += ruta

        // Contrato de seguridad: SIEMPRE relativo bajo filesDir, nunca absoluto.
        assertFalse(File(ruta).isAbsolute)
        assertTrue(ruta.startsWith("fotos/"))
        assertFalse(ruta.contains(context.filesDir.absolutePath))
        assertTrue(ruta.endsWith(".jpg"))

        val fichero = File(context.filesDir, ruta)
        assertTrue(fichero.exists())
        assertArrayEquals(jpeg, fichero.readBytes())
    }

    @Test
    fun saveGeneratesUniqueRelativeNamesPerCall() = runBlocking {
        val ruta1 = store.save(byteArrayOf(1))
        val ruta2 = store.save(byteArrayOf(2))
        guardados += ruta1
        guardados += ruta2

        assertFalse(File(ruta1).isAbsolute)
        assertFalse(File(ruta2).isAbsolute)
        assertTrue(ruta1.startsWith("fotos/"))
        assertTrue(ruta2.startsWith("fotos/"))
        assertFalse(ruta1 == ruta2)
        assertTrue(File(context.filesDir, ruta1).exists())
        assertTrue(File(context.filesDir, ruta2).exists())
    }

    @Test
    fun saveThenDeleteRemovesTheFile() = runBlocking {
        val ruta = store.save(byteArrayOf(7, 7, 7))
        val fichero = File(context.filesDir, ruta)
        assertTrue(fichero.exists())

        val borrado = store.delete(ruta)

        assertTrue(borrado)
        assertFalse(fichero.exists())
    }

    @Test
    fun savedPhotoSurvivesProcessDeath() = runBlocking {
        // Escenario 2 del spec: la foto esta en disco en el momento de la
        // captura (nombre relativo); aqui se comprueba que el fichero existe
        // fisicamente bajo filesDir (la persistencia tras force-stop se
        // verifica en el dispositivo).
        val ruta = store.save(byteArrayOf(5, 5))
        guardados += ruta

        val fichero = File(context.filesDir, ruta)
        assertEquals("fotos/${fichero.name}", ruta)
        assertTrue(fichero.isFile && fichero.length() == 2L)
    }

    // --- read (spec ocr-mlkit): la foto ya guardada se lee para el OCR ---

    @Test
    fun readReturnsTheStoredBytesGivenRelativeName() = runBlocking {
        val ruta = store.save(byteArrayOf(10, 20, 30))
        guardados += ruta

        val bytes = store.read(ruta)

        assertArrayEquals(byteArrayOf(10, 20, 30), bytes)
    }

    @Test
    fun readReturnsTheStoredBytesGivenAbsolutePath() = runBlocking {
        val ruta = store.save(byteArrayOf(40, 50))
        guardados += ruta

        val bytes = store.read(File(context.filesDir, ruta).absolutePath)

        assertArrayEquals(byteArrayOf(40, 50), bytes)
    }

    @Test
    fun readReturnsNullForMissingFileWithoutCrashing() = runBlocking {
        assertNull(store.read("fotos/no_existe.jpg"))
    }

    @Test
    fun readReturnsNullForBlankPath() = runBlocking {
        assertNull(store.read(""))
    }
}
