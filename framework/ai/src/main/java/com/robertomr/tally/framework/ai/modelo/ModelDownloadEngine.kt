package com.robertomr.tally.framework.ai.modelo

import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** Resultado de [ModelDownloadEngine.descargar] (los fallos de red lanzan [IOException]). */
internal sealed interface ResultadoMotor {
    /** El fichero final existe con [bytes] bytes (rename atomico hecho). */
    data class Completado(val bytes: Long) : ResultadoMotor

    /** 401/403: falta aceptar la licencia o el token no es valido. */
    data object Autorizacion : ResultadoMotor

    /** El tamano final no coincide con el anunciado; el `.part` ya se descarto. */
    data class Integridad(val esperado: Long, val obtenido: Long) : ResultadoMotor

    /** Otro 4xx: no tiene sentido reintentar. */
    data class ErrorHttp(val codigo: Int) : ResultadoMotor
}

/**
 * Cliente de descarga reanudable sobre `HttpURLConnection` (spec
 * descarga-modelo; sin Retrofit/OkHttp). Escribe en `<destino>.part` y hace
 * rename atomico al terminar.
 *
 * - Redirecciones manuales (hasta [MAX_SALTOS]): la cabecera `Authorization`
 *   solo se envia al host original, nunca al CDN (R-DM2).
 * - Si hay `.part` con N bytes pide `Range: bytes=N-`: 206 -> append; 200 -> el
 *   servidor ignoro el Range, se trunca y se reinicia limpio; 416 con N == total
 *   -> completar; 416 en otro caso -> reiniciar.
 * - `IOException` (corte de red, 5xx) se propaga: el llamador decide reintentar
 *   y el `.part` se conserva para reanudar. La cancelacion de la corrutina
 *   tambien conserva el `.part`.
 *
 * Solo HTTPS salvo que [soloHttps] sea `false` (servidor local de los tests).
 */
internal class ModelDownloadEngine(
    private val url: String,
    private val destino: File,
    private val token: String?,
    private val soloHttps: Boolean = true,
    private val bloqueBytes: Int = 256 * 1024,
    private val intervaloProgresoMs: Long = 500L,
    private val log: (String) -> Unit = {},
) {

    val parcial: File = File(destino.path + ".part")

    /** Bytes del `.part` con los que arranco la ultima descarga (evidencia de reanudacion). */
    var bytesIniciales: Long = 0L
        private set

    suspend fun descargar(
        onProgreso: suspend (bytes: Long, total: Long) -> Unit = { _, _ -> },
    ): ResultadoMotor = withContext(Dispatchers.IO) {
        destino.absoluteFile.parentFile?.mkdirs()
        var reinicios = 0
        while (true) {
            currentCoroutineContext().ensureActive()
            val previos = if (parcial.isFile) parcial.length() else 0L
            bytesIniciales = previos
            val conexion = conectar(previos)
            try {
                val codigo = conexion.responseCode
                when {
                    codigo == HttpURLConnection.HTTP_PARTIAL -> {
                        val rango = RangoContenido.parsear(conexion.getHeaderField("Content-Range"))
                        if (rango == null || rango.inicio != previos) {
                            // Respuesta incoherente con lo pedido: reinicio limpio.
                            if (reinicios++ >= 1) throw IOException("Content-Range incoherente")
                            parcial.delete()
                            continue
                        }
                        log("Reanudando desde $previos bytes (Range)")
                        return@withContext volcar(conexion, previos, rango.total, true, onProgreso)
                    }
                    codigo == HttpURLConnection.HTTP_OK -> {
                        if (previos > 0) log("El servidor ignoro el Range (200): reinicio limpio")
                        parcial.delete()
                        bytesIniciales = 0L
                        return@withContext volcar(
                            conexion, 0L, conexion.contentLengthLong, false, onProgreso,
                        )
                    }
                    codigo == HTTP_RANGE_NOT_SATISFIABLE -> {
                        val total = RangoContenido.totalDeInsatisfecho(
                            conexion.getHeaderField("Content-Range"),
                        )
                        if (previos > 0 && total == previos) {
                            return@withContext finalizar(previos)
                        }
                        if (reinicios++ >= 1) return@withContext ResultadoMotor.ErrorHttp(codigo)
                        parcial.delete()
                        continue
                    }
                    codigo == HttpURLConnection.HTTP_UNAUTHORIZED ||
                        codigo == HttpURLConnection.HTTP_FORBIDDEN ->
                        return@withContext ResultadoMotor.Autorizacion
                    codigo in 400..499 -> return@withContext ResultadoMotor.ErrorHttp(codigo)
                    else -> throw IOException("HTTP $codigo")
                }
            } finally {
                conexion.disconnect()
            }
        }
        error("inalcanzable")
    }

    private suspend fun volcar(
        conexion: HttpURLConnection,
        previos: Long,
        total: Long,
        anadir: Boolean,
        onProgreso: suspend (Long, Long) -> Unit,
    ): ResultadoMotor {
        var escritos = previos
        var ultimoAviso = System.nanoTime()
        conexion.inputStream.use { entrada ->
            java.io.FileOutputStream(parcial, anadir).use { salida ->
                val buffer = ByteArray(bloqueBytes)
                onProgreso(escritos, total)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val leidos = entrada.read(buffer)
                    if (leidos < 0) break
                    salida.write(buffer, 0, leidos)
                    escritos += leidos
                    val ahora = System.nanoTime()
                    if ((ahora - ultimoAviso) / 1_000_000 >= intervaloProgresoMs) {
                        ultimoAviso = ahora
                        onProgreso(escritos, total)
                    }
                }
            }
        }
        onProgreso(escritos, total)
        if (total >= 0 && escritos != total) {
            parcial.delete()
            return ResultadoMotor.Integridad(total, escritos)
        }
        return finalizar(escritos)
    }

    private fun finalizar(bytes: Long): ResultadoMotor {
        Files.move(
            parcial.toPath(),
            destino.toPath(),
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING,
        )
        return ResultadoMotor.Completado(bytes)
    }

    /** Abre la conexion siguiendo las redirecciones a mano y sin filtrar el token al CDN. */
    private fun conectar(desde: Long): HttpURLConnection {
        val original = URL(url)
        var actual = original
        repeat(MAX_SALTOS + 1) {
            if (soloHttps && actual.protocol != "https") {
                throw IOException("Solo se permite HTTPS")
            }
            val conexion = actual.openConnection() as HttpURLConnection
            conexion.instanceFollowRedirects = false
            conexion.connectTimeout = TIMEOUT_MS
            conexion.readTimeout = TIMEOUT_MS
            if (!token.isNullOrBlank() && actual.host == original.host &&
                actual.port == original.port
            ) {
                conexion.setRequestProperty("Authorization", "Bearer $token")
            }
            if (desde > 0) conexion.setRequestProperty("Range", "bytes=$desde-")
            val codigo = conexion.responseCode
            if (codigo in REDIRECCIONES) {
                val destinoRedireccion = conexion.getHeaderField("Location")
                conexion.disconnect()
                if (destinoRedireccion.isNullOrBlank()) throw IOException("Redireccion sin Location")
                actual = URL(actual, destinoRedireccion)
                return@repeat
            }
            return conexion
        }
        throw IOException("Demasiadas redirecciones")
    }

    private companion object {
        const val MAX_SALTOS = 5
        const val TIMEOUT_MS = 30_000
        const val HTTP_RANGE_NOT_SATISFIABLE = 416
        val REDIRECCIONES = setOf(301, 302, 303, 307, 308)
    }
}

/** Parseo de `Content-Range` (`bytes 100-199/1000` y `bytes * / 1000` sin espacios). */
internal class RangoContenido(val inicio: Long, val total: Long) {
    companion object {
        private val RANGO = Regex("""bytes\s+(\d+)-(\d+)/(\d+|\*)""")
        private val INSATISFECHO = Regex("""bytes\s+\*/(\d+)""")

        fun parsear(cabecera: String?): RangoContenido? {
            if (cabecera == null) return null
            val m = RANGO.matchEntire(cabecera.trim()) ?: return null
            val total = m.groupValues[3].toLongOrNull() ?: -1L
            return RangoContenido(m.groupValues[1].toLong(), total)
        }

        fun totalDeInsatisfecho(cabecera: String?): Long? {
            if (cabecera == null) return null
            return INSATISFECHO.matchEntire(cabecera.trim())?.groupValues?.get(1)?.toLong()
        }
    }
}
