package com.robertomr.tally.framework.ai.modelo

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * El motor de descarga sobre la implementacion REAL de `HttpURLConnection` de
 * Android (distinta de la del JDK que usan los tests JVM): reanudacion con
 * `Range`, servidor que ignora Range y redireccion a otro origen sin
 * `Authorization` (spec descarga-modelo, T4/T8). Servidor local minimo con
 * `ServerSocket`.
 */
@RunWith(AndroidJUnit4::class)
class ModelDownloadEngineDeviceTest {

    private class Pedido(val ruta: String, val range: String?, val authorization: String?)

    private val peticiones = CopyOnWriteArrayList<Pedido>()
    private val sockets = mutableListOf<ServerSocket>()
    private val contenido = ByteArray(3_000_000) { (it * 17 + 3).toByte() }
    private lateinit var carpeta: File
    private lateinit var destino: File

    @Before
    fun preparar() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        carpeta = File(ctx.cacheDir, "motor-test").also {
            it.deleteRecursively()
            it.mkdirs()
        }
        destino = File(carpeta, "modelo.litertlm")
    }

    @After
    fun limpiar() {
        sockets.forEach { runCatching { it.close() } }
        carpeta.deleteRecursively()
    }

    /** Servidor HTTP/1.1 minimo: [responder] devuelve cabeceras + cuerpo por pedido. */
    private fun servidor(
        responder: (Pedido) -> Triple<String, List<String>, ByteArray>,
    ): Int {
        val ss = ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"))
        sockets += ss
        Thread {
            while (!ss.isClosed) {
                val s = runCatching { ss.accept() }.getOrNull() ?: break
                Thread { atender(s, responder) }.start()
            }
        }.start()
        return ss.localPort
    }

    private fun atender(s: Socket, responder: (Pedido) -> Triple<String, List<String>, ByteArray>) {
        s.use {
            val lector = it.getInputStream().bufferedReader(Charsets.ISO_8859_1)
            val linea = lector.readLine() ?: return
            val cabeceras = mutableMapOf<String, String>()
            while (true) {
                val l = lector.readLine() ?: break
                if (l.isEmpty()) break
                val i = l.indexOf(':')
                cabeceras[l.substring(0, i).lowercase()] = l.substring(i + 1).trim()
            }
            val pedido = Pedido(
                linea.split(" ")[1].substringBefore('?'),
                cabeceras["range"],
                cabeceras["authorization"],
            )
            peticiones += pedido
            val (estado, extra, cuerpo) = responder(pedido)
            val salida = it.getOutputStream()
            val cab = buildString {
                append("HTTP/1.1 $estado\r\n")
                extra.forEach { h -> append(h).append("\r\n") }
                append("Content-Length: ${cuerpo.size}\r\nConnection: close\r\n\r\n")
            }
            salida.write(cab.toByteArray(Charsets.ISO_8859_1))
            runCatching {
                salida.write(cuerpo)
                salida.flush()
            }
        }
    }

    private fun conRange(p: Pedido): Triple<String, List<String>, ByteArray> {
        val inicio = p.range?.removePrefix("bytes=")?.removeSuffix("-")?.toInt()
        return if (inicio == null) {
            Triple("200 OK", emptyList(), contenido)
        } else {
            Triple(
                "206 Partial Content",
                listOf("Content-Range: bytes $inicio-${contenido.size - 1}/${contenido.size}"),
                contenido.copyOfRange(inicio, contenido.size),
            )
        }
    }

    private fun motor(puerto: Int, token: String? = null) = ModelDownloadEngine(
        url = "http://127.0.0.1:$puerto/modelo",
        destino = destino,
        token = token,
        soloHttps = false,
        intervaloProgresoMs = 0,
    )

    @Test
    fun reanudaConRangeDesdeElParcial() = runBlocking {
        val puerto = servidor(::conRange)
        File(destino.path + ".part").writeBytes(contenido.copyOfRange(0, 1_200_000))
        val motor = motor(puerto)

        val r = motor.descargar()

        assertEquals(ResultadoMotor.Completado(contenido.size.toLong()), r)
        assertEquals("bytes=1200000-", peticiones.single().range)
        assertEquals(1_200_000L, motor.bytesIniciales)
        assertTrue(destino.readBytes().contentEquals(contenido))
        assertFalse(File(destino.path + ".part").exists())
    }

    @Test
    fun servidorQueIgnoraRangeReiniciaLimpio() = runBlocking {
        val puerto = servidor { Triple("200 OK", emptyList(), contenido) }
        File(destino.path + ".part").writeBytes(ByteArray(500_000) { 1 })
        val motor = motor(puerto)

        val r = motor.descargar()

        assertEquals(ResultadoMotor.Completado(contenido.size.toLong()), r)
        assertEquals("bytes=500000-", peticiones.single().range)
        assertEquals(0L, motor.bytesIniciales)
        assertTrue(destino.readBytes().contentEquals(contenido))
    }

    @Test
    fun redireccionAOtroOrigenNoEnviaAuthorization() = runBlocking {
        val cdn = servidor(::conRange)
        val origen = servidor {
            Triple("302 Found", listOf("Location: http://127.0.0.1:$cdn/cdn?firma=1"), ByteArray(0))
        }

        val r = motor(origen, token = "hf_secreto").descargar()

        assertEquals(ResultadoMotor.Completado(contenido.size.toLong()), r)
        assertEquals("Bearer hf_secreto", peticiones.first { it.ruta == "/modelo" }.authorization)
        assertNull(peticiones.first { it.ruta == "/cdn" }.authorization)
    }

    @Test
    fun codigo401DevuelveAutorizacion() = runBlocking {
        val puerto = servidor { Triple("401 Unauthorized", emptyList(), ByteArray(0)) }
        assertEquals(ResultadoMotor.Autorizacion, motor(puerto).descargar())
        assertFalse(destino.exists())
    }
}
