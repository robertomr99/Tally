package com.robertomr.tally.feature.escanear

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** Tope de lectura de la imagen de galeria (spec galeria-escaneo), sin Android. */
class LecturaImagenTest {

    @Test
    fun leeTodoLoQueCabeEnElTope() {
        val datos = ByteArray(200_000) { (it % 251).toByte() }

        val leido = leerAcotado(ByteArrayInputStream(datos), maxBytes = 200_000)

        assertArrayEquals(datos, leido)
    }

    @Test
    fun abortaAlSuperarElTope() {
        val datos = ByteArray(200_001)

        assertThrows(ImagenDemasiadoGrandeException::class.java) {
            leerAcotado(ByteArrayInputStream(datos), maxBytes = 200_000)
        }
    }

    @Test
    fun noLeeMasAllaDelBloqueQueSuperaElTope() {
        // Flujo "infinito": el tope corta la lectura sin agotar memoria.
        val infinito = object : InputStream() {
            override fun read(): Int = 1
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                b.fill(1, off, off + len)
                return len
            }
        }

        assertThrows(ImagenDemasiadoGrandeException::class.java) {
            leerAcotado(infinito, maxBytes = 100_000)
        }
    }

    @Test
    fun imagenVaciaDevuelveArrayVacio() {
        assertEquals(0, leerAcotado(ByteArrayInputStream(ByteArray(0)), 10).size)
    }

    @Test
    fun mensajesSegunElFallo() {
        assertEquals(MENSAJE_IMAGEN_GRANDE, mensajeDeLectura(ImagenDemasiadoGrandeException(1)))
        assertEquals(MENSAJE_IMAGEN_GRANDE, mensajeDeLectura(OutOfMemoryError()))
        assertEquals(MENSAJE_IMAGEN_ILEGIBLE, mensajeDeLectura(IOException("x")))
        assertEquals(MENSAJE_IMAGEN_ILEGIBLE, mensajeDeLectura(SecurityException("x")))
    }
}
