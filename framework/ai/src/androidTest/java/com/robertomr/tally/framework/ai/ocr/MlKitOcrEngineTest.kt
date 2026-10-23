package com.robertomr.tally.framework.ai.ocr

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentados de [MlKitOcrEngine] con ML Kit Text Recognition v2
 * latin bundled (spec ocr-mlkit): el OCR corre realmente on-device.
 *
 * (a) Imagen renderizada en runtime (Canvas: texto grande y claro) con asserts
 *     de tokens exactos — re-ejecutable sin fixtures.
 * (b) HARNESS de los fixtures reales de tickets: `ticket_real_1..4.jpg` con su
 *     tabla de tokens esperados en `ticket_real_1..4.tokens.txt` (un token por
 *     linea; tokens "razonables" de comercio/total del ticket, no la
 *     transcripcion literal). Cada metodo lee el asset y, si el fixture o su
 *     fichero de tokens no existe, se SALTA con `assumeTrue` y motivo explicito
 *     (sin inventar fixtures ni resultados). Desviacion registrada (2026-10-16):
 *     el spec planificaba 5 tickets, pero solo se aportaron 4 fotos reales; se
 *     acepta la desviacion y el harness cubre 4 (ver Desviaciones del spec
 *     ocr-mlkit).
 *
 * El texto crudo reconocido se loguea con el tag `MlKitOcr` para exportarlo a
 * `docs/evidence/ocr-mlkit/` como evidencia.
 */
@RunWith(AndroidJUnit4::class)
class MlKitOcrEngineTest {

    private val engine = MlKitOcrEngine()

    @Test
    fun renderedTicketImageYieldsTheExpectedExactTokens() = runBlocking {
        val jpeg = renderizarJpeg(
            lineas = listOf("MERCADONA", "TICKET", "TOTAL", "12,34 EUR"),
        )

        val texto = engine.recognize(jpeg)
        Log.i(TAG, "texto_crudo_render=<<<$texto>>>")

        assertTrue("el OCR devolvio un texto vacio: <<<$texto>>>", texto.isNotBlank())
        // Tokens exactos: palabras enteras del render, tal cual en el texto crudo.
        listOf("MERCADONA", "TICKET", "TOTAL", "EUR").forEach { token ->
            assertTrue("falta el token '$token' en <<<$texto>>>", texto.contains(token))
        }
    }

    @Test
    fun imageWithoutTextReturnsEmptyRawText() = runBlocking {
        val jpeg = renderizarJpeg(lineas = emptyList())

        val texto = engine.recognize(jpeg)

        assertEquals("", texto.trim())
    }

    @Test
    fun undecodableBytesReturnEmptyRawTextWithoutCrashing() = runBlocking {
        val texto = engine.recognize(byteArrayOf(1, 2, 3, 4))

        assertEquals("", texto)
    }

    // --- Harness de fixtures reales (T6b): se saltan si el fixture no existe ---
    // Los tickets de papel y sus tokens esperados son aportacion humana. El
    // harness queda listo y re-ejecutable: al anadir `ticket_real_<n>.jpg` y
    // `ticket_real_<n>.tokens.txt` en `src/androidTest/assets/`, estos tests se
    // ejecutan de verdad. Hoy cubre 4 tickets (desviacion 2026-10-16 respecto a
    // los 5 planificados).

    @Test
    fun realTicket1RawTextContainsExpectedTokens() = verificarFixtureReal(1)

    @Test
    fun realTicket2RawTextContainsExpectedTokens() = verificarFixtureReal(2)

    @Test
    fun realTicket3RawTextContainsExpectedTokens() = verificarFixtureReal(3)

    @Test
    fun realTicket4RawTextContainsExpectedTokens() = verificarFixtureReal(4)

    /**
     * Corre el OCR sobre el fixture real `ticket_real_<n>.jpg` y comprueba que
     * el texto crudo contiene todos los tokens esperados del fichero
     * `ticket_real_<n>.tokens.txt`. Si falta cualquiera de los dos assets, el
     * test se SALTA (assumption) con el motivo: no se inventan tickets ni
     * resultados.
     */
    private fun verificarFixtureReal(numero: Int): Unit = runBlocking {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        val jpeg = leerAssetOLanzarSkip(assets, "ticket_real_$numero.jpg") ?: return@runBlocking
        val tokensEsperados = leerTokensOLanzarSkip(assets, "ticket_real_$numero.tokens.txt")
            ?: return@runBlocking

        val texto = engine.recognize(jpeg)
        Log.i(TAG, "texto_crudo_real_$numero=<<<$texto>>>")

        assertTrue(
            "el OCR del fixture real $numero devolvio un texto vacio: <<<$texto>>>",
            texto.isNotBlank(),
        )
        tokensEsperados.forEach { token ->
            assertTrue(
                "falta el token esperado '$token' en el fixture real $numero: <<<$texto>>>",
                texto.contains(token),
            )
        }
    }

    private fun leerAssetOLanzarSkip(assets: AssetManager, nombre: String): ByteArray? =
        try {
            assets.open(nombre).use { it.readBytes() }
        } catch (_: IOException) {
            Assume.assumeTrue(
                "Fixture real '$nombre' no presente en androidTest/assets: los " +
                    "tickets de papel los aporta mano humana (ver Desviaciones del " +
                    "spec ocr-mlkit).",
                false,
            )
            null
        }

    private fun leerTokensOLanzarSkip(assets: AssetManager, nombre: String): List<String>? =
        try {
            assets.open(nombre).use { flujo ->
                flujo.bufferedReader().readLines().map { it.trim() }.filter { it.isNotEmpty() }
            }
        } catch (_: IOException) {
            Assume.assumeTrue(
                "Tokens esperados '$nombre' no presentes: sin la tabla de tokens " +
                    "del ticket real no se inventan asserts (el fixture se aporta " +
                    "con su tabla; ver Desviaciones del spec ocr-mlkit).",
                false,
            )
            null
        }

    /**
     * Renderiza en runtime un "ticket" sintetico (fondo blanco, texto negro
     * grande) y lo comprime a JPEG: no depende de fixtures del repo.
     */
    private fun renderizarJpeg(lineas: List<String>): ByteArray {
        val ancho = 900
        val alto = 600
        val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 72f
            isAntiAlias = true
        }
        var y = 120f
        lineas.forEach { linea ->
            canvas.drawText(linea, 48f, y, paint)
            y += 110f
        }
        val salida = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, salida)
        return salida.toByteArray()
    }

    private companion object {
        const val TAG = "MlKitOcr"
    }
}
