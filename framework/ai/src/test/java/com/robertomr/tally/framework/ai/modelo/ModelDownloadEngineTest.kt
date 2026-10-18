package com.robertomr.tally.framework.ai.modelo

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Tests JVM de [ModelDownloadEngine] contra servidores locales `HttpServer` del
 * JDK (spec descarga-modelo, T4): 200 completo, reanudacion 206, servidor que
 * ignora Range, 416, 401/403, redireccion cross-host SIN Authorization,
 * longitud truncada, cancelacion que deja el `.part` y solo-HTTPS.
 */
class ModelDownloadEngineTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    private val servidores = mutableListOf<HttpServer>()
    private val peticiones = CopyOnWriteArrayList<Pedido>()

    private class Pedido(val ruta: String, val range: String?, val authorization: String?)

    private lateinit var destino: File
    private val contenido = ByteArray(1_000_000) { (it * 31 + 7).toByte() }

    @Before
    fun preparar() {
        destino = File(carpeta.newFolder("models"), "modelo.litertlm")
    }

    @After
    fun parar() {
        servidores.forEach { it.stop(0) }
    }

    private fun servidor(manejador: (HttpExchange) -> Unit): HttpServer {
        val s = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        s.createContext("/") { ex ->
            peticiones += Pedido(
                ex.requestURI.path,
                ex.requestHeaders.getFirst("Range"),
                ex.requestHeaders.getFirst("Authorization"),
            )
            try {
                manejador(ex)
            } finally {
                ex.close()
            }
        }
        s.start()
        servidores += s
        return s
    }

    private fun HttpServer.url(ruta: String = "/modelo") =
        "http://127.0.0.1:${address.port}$ruta"

    /** Servidor que sirve [contenido] respetando `Range: bytes=N-`. */
    private fun servidorConRange(datos: ByteArray = contenido) = servidor { ex ->
        val range = ex.requestHeaders.getFirst("Range")
        if (range == null) {
            ex.sendResponseHeaders(200, datos.size.toLong())
            ex.responseBody.write(datos)
        } else {
            val inicio = range.removePrefix("bytes=").removeSuffix("-").toInt()
            if (inicio >= datos.size) {
                ex.responseHeaders.add("Content-Range", "bytes */${datos.size}")
                ex.sendResponseHeaders(416, -1)
            } else {
                ex.responseHeaders.add(
                    "Content-Range", "bytes $inicio-${datos.size - 1}/${datos.size}",
                )
                ex.sendResponseHeaders(206, (datos.size - inicio).toLong())
                ex.responseBody.write(datos, inicio, datos.size - inicio)
            }
        }
    }

    private fun motor(s: HttpServer, token: String? = null, ruta: String = "/modelo") =
        ModelDownloadEngine(
            url = s.url(ruta),
            destino = destino,
            token = token,
            soloHttps = false,
            bloqueBytes = 16 * 1024,
            intervaloProgresoMs = 0,
        )

    @Test
    fun descargaCompletaEscribeElFicheroFinalSinParcial() = runBlocking {
        val s = servidorConRange()
        val progresos = mutableListOf<Long>()

        val r = motor(s, token = "hf_secreto").descargar { bytes, total ->
            progresos += bytes
            assertEquals(contenido.size.toLong(), total)
        }

        assertEquals(ResultadoMotor.Completado(contenido.size.toLong()), r)
        assertTrue(destino.readBytes().contentEquals(contenido))
        assertFalse(File(destino.path + ".part").exists())
        assertTrue(progresos.size > 2)
        assertEquals(contenido.size.toLong(), progresos.last())
        assertEquals("Bearer hf_secreto", peticiones.single().authorization)
        assertNull(peticiones.single().range)
    }

    @Test
    fun conParcialReanudaConRangeYAnade() = runBlocking {
        val s = servidorConRange()
        File(destino.path + ".part").writeBytes(contenido.copyOfRange(0, 400_000))
        val motor = motor(s)

        val r = motor.descargar()

        assertEquals(ResultadoMotor.Completado(contenido.size.toLong()), r)
        assertEquals("bytes=400000-", peticiones.single().range)
        assertEquals(400_000L, motor.bytesIniciales)
        assertTrue(destino.readBytes().contentEquals(contenido))
        assertFalse(File(destino.path + ".part").exists())
    }

    @Test
    fun servidorQueIgnoraRangeReiniciaLimpio() = runBlocking {
        // Siempre responde 200 con el cuerpo completo, aunque se pida Range.
        val s = servidor { ex ->
            ex.sendResponseHeaders(200, contenido.size.toLong())
            ex.responseBody.write(contenido)
        }
        File(destino.path + ".part").writeBytes(ByteArray(300_000) { 1 })
        val motor = motor(s)

        val r = motor.descargar()

        assertEquals(ResultadoMotor.Completado(contenido.size.toLong()), r)
        assertEquals("bytes=300000-", peticiones.single().range)
        assertEquals(0L, motor.bytesIniciales)
        assertTrue(destino.readBytes().contentEquals(contenido))
    }

    @Test
    fun parcialYaCompletoRecibe416YSeCompleta() = runBlocking {
        val s = servidorConRange()
        File(destino.path + ".part").writeBytes(contenido)

        val r = motor(s).descargar()

        assertEquals(ResultadoMotor.Completado(contenido.size.toLong()), r)
        assertEquals("bytes=1000000-", peticiones.single().range)
        assertTrue(destino.readBytes().contentEquals(contenido))
        assertFalse(File(destino.path + ".part").exists())
    }

    @Test
    fun parcialMayorQueElTotalRecibe416YReiniciaLimpio() = runBlocking {
        val s = servidorConRange()
        File(destino.path + ".part").writeBytes(ByteArray(contenido.size + 50) { 9 })

        val r = motor(s).descargar()

        assertEquals(ResultadoMotor.Completado(contenido.size.toLong()), r)
        assertEquals(2, peticiones.size)
        assertNull(peticiones[1].range)
        assertTrue(destino.readBytes().contentEquals(contenido))
    }

    @Test
    fun codigos401y403DevuelvenAutorizacionSinTocarElFichero() = runBlocking {
        for (codigo in listOf(401, 403)) {
            val s = servidor { ex -> ex.sendResponseHeaders(codigo, -1) }
            assertEquals(ResultadoMotor.Autorizacion, motor(s).descargar())
        }
        assertFalse(destino.exists())
    }

    @Test
    fun otro4xxEsErrorHttpYElCincoXxEsIOException() = runBlocking {
        val s404 = servidor { ex -> ex.sendResponseHeaders(404, -1) }
        assertEquals(ResultadoMotor.ErrorHttp(404), motor(s404).descargar())

        val s503 = servidor { ex -> ex.sendResponseHeaders(503, -1) }
        try {
            motor(s503).descargar()
            org.junit.Assert.fail("Debia lanzar IOException")
        } catch (_: IOException) {
            // esperado: el worker reintenta
        }
    }

    @Test
    fun redireccionAOtroHostNoEnviaAuthorizationPeroSiAlOriginal() = runBlocking {
        val cdn = servidorConRange()
        // "otro host": mismo loopback, distinto puerto (host:puerto distinto = otro origen).
        val origen = servidor { ex ->
            ex.responseHeaders.add("Location", cdn.url("/cdn/modelo?firma=abc"))
            ex.sendResponseHeaders(302, -1)
        }

        val r = motor(origen, token = "hf_secreto").descargar()

        assertEquals(ResultadoMotor.Completado(contenido.size.toLong()), r)
        val alOrigen = peticiones.first { it.ruta == "/modelo" }
        val alCdn = peticiones.first { it.ruta == "/cdn/modelo" }
        assertEquals("Bearer hf_secreto", alOrigen.authorization)
        assertNull("El token no debe viajar al CDN", alCdn.authorization)
    }

    @Test
    fun redireccionRelativaMismoHostConservaAuthorization() = runBlocking {
        val s = servidor { ex ->
            if (ex.requestURI.path == "/modelo") {
                ex.responseHeaders.add("Location", "/real")
                ex.sendResponseHeaders(307, -1)
            } else {
                ex.sendResponseHeaders(200, contenido.size.toLong())
                ex.responseBody.write(contenido)
            }
        }

        val r = motor(s, token = "hf_secreto").descargar()

        assertEquals(ResultadoMotor.Completado(contenido.size.toLong()), r)
        assertEquals("Bearer hf_secreto", peticiones.first { it.ruta == "/real" }.authorization)
    }

    @Test
    fun bucleDeRedireccionesFallaConIOException() = runBlocking {
        val s = servidor { ex ->
            ex.responseHeaders.add("Location", "/modelo")
            ex.sendResponseHeaders(302, -1)
        }
        try {
            motor(s).descargar()
            org.junit.Assert.fail("Debia lanzar IOException")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("redirecciones"))
        }
        assertEquals(6, peticiones.size)
    }

    @Test
    fun cuerpoMasCortoQueElAnunciadoNoQuedaComoDescargado() = runBlocking {
        // Anuncia 1.000.000 bytes y corta a mitad: nunca se promueve a fichero final.
        val s = servidor { ex ->
            ex.sendResponseHeaders(200, contenido.size.toLong())
            ex.responseBody.write(contenido, 0, 400_000)
            ex.responseBody.flush()
        }
        val resultado = try {
            motor(s).descargar()
        } catch (e: IOException) {
            e
        }

        assertFalse("El fichero final no debe existir", destino.exists())
        when (resultado) {
            is ResultadoMotor.Integridad -> {
                assertEquals(contenido.size.toLong(), resultado.esperado)
                assertFalse(File(destino.path + ".part").exists())
            }
            is IOException ->
                // Corte a mitad: el parcial se conserva para reanudar.
                assertTrue(File(destino.path + ".part").length() in 1..400_000)
            else -> org.junit.Assert.fail("Resultado inesperado: $resultado")
        }
    }

    @Test
    fun cancelarConservaElParcial() = runBlocking {
        val primerBloque = CompletableDeferred<Unit>()
        val liberar = java.util.concurrent.CountDownLatch(1)
        val s = servidor { ex ->
            ex.sendResponseHeaders(200, contenido.size.toLong())
            ex.responseBody.write(contenido, 0, 200_000)
            ex.responseBody.flush()
            liberar.await(10, java.util.concurrent.TimeUnit.SECONDS)
            runCatching { ex.responseBody.write(contenido, 200_000, 800_000) }
        }
        val job = launch(Dispatchers.Default) {
            motor(s).descargar { bytes, _ ->
                if (bytes > 0) primerBloque.complete(Unit)
            }
        }

        withTimeout(10_000) { primerBloque.await() }
        // Dejar que el motor escriba el primer bloque antes de cancelar y liberar al servidor.
        kotlinx.coroutines.delay(300)
        liberar.countDown()
        job.cancelAndJoin()

        assertFalse(destino.exists())
        val parcial = File(destino.path + ".part")
        assertTrue(parcial.exists())
        assertTrue(parcial.length() in 1..contenido.size.toLong())
    }

    @Test
    fun soloHttpsRechazaUrlsHttp() = runBlocking {
        val s = servidorConRange()
        val motor = ModelDownloadEngine(url = s.url(), destino = destino, token = "x")
        try {
            motor.descargar()
            org.junit.Assert.fail("Debia rechazar http")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("HTTPS"))
        }
        assertEquals(0, peticiones.size)
    }

    @Test
    fun parseaContentRange() {
        val r = RangoContenido.parsear("bytes 400000-999999/1000000")!!
        assertEquals(400_000L, r.inicio)
        assertEquals(1_000_000L, r.total)
        assertNull(RangoContenido.parsear(null))
        assertNull(RangoContenido.parsear("basura"))
        assertEquals(1000L, RangoContenido.totalDeInsatisfecho("bytes */1000"))
    }
}
