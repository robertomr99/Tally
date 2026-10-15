package com.robertomr.tally.framework.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.robertomr.tally.framework.database.source.PhotoFileStoreImpl
import java.io.File
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentados de [PhotoFileStoreImpl] con un fichero real en
 * `filesDir` (spec persistencia-room, escenario 2: borrado best-effort de la
 * foto del ticket).
 */
@RunWith(AndroidJUnit4::class)
class PhotoFileStoreTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val store = PhotoFileStoreImpl(context)
    private val testFile = File(context.filesDir, "photo-file-store-test.jpg")

    @Before
    fun setUp() {
        testFile.writeBytes(byteArrayOf(1, 2, 3))
    }

    @After
    fun tearDown() {
        testFile.delete()
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
}
