package com.robertomr.tally.extraccion

import android.os.Looper
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.robertomr.tally.di.TextEngineEntryPoint
import com.robertomr.tally.domain.expenses.data.TextEngine
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.ModeloLocalNoDisponibleException
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.TicketExtraido
import dagger.hilt.android.EntryPointAccessors
import java.io.IOException
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test REAL de la extraccion LOCAL (spec extraccion-local, T5/T7 = verificaciones
 * 1 y 3): corre el `TextEngine` `@Named("local")` (Gemma 3n via MediaPipe) sobre
 * los 4 textos crudos reales (`assets/texto_crudo_1..4.txt`, los mismos que la nube).
 *
 * Criterio duro: la salida pasa el parser compartido, `motor == LOCAL`, la
 * categoria es de la enum y no hay crash. El total frente al papel (35,30 / 3,50 /
 * 13,90 / 7,91) se REGISTRA en logcat (tag `ExtraccionLocal`), no es assert duro
 * (D5 del spec). La latencia la loguea el runner `MediaPipeLocalLlm`
 * (`loadMillis`, `generationMillis`, tokens, `ranOnMainThread`).
 *
 * Sin modelo descargado se SALTA con `assumeTrue`. Ejecutar con
 * `adb shell am instrument` (NO `connected*AndroidTest`: desinstala la app y borra
 * el modelo de 3,4 GB), ver docs/evidence/extraccion-local/.
 */
@RunWith(AndroidJUnit4::class)
class ExtraccionLocalRealTest {

    private val textEngine: TextEngine by lazy {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        EntryPointAccessors
            .fromApplication(contexto, TextEngineEntryPoint::class.java)
            .textEngineLocal()
    }

    @Test
    fun theInjectedEngineIsTheLocalOneAndTheDefaultStaysCloud() {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val ep = EntryPointAccessors.fromApplication(contexto, TextEngineEntryPoint::class.java)
        assertEquals(Motor.LOCAL, ep.textEngineLocal().motor)
        assertEquals(Motor.NUBE, ep.textEngine().motor)
    }

    @Test
    fun ticket1() = extraerYRegistrar(1, 35.30)

    @Test
    fun ticket2() = extraerYRegistrar(2, 3.50)

    @Test
    fun ticket3() = extraerYRegistrar(3, 13.90)

    @Test
    fun ticket4() = extraerYRegistrar(4, 7.91)

    private fun extraerYRegistrar(numero: Int, totalPapel: Double): Unit = runBlocking {
        assertFalse("el test no debe correr en el hilo principal", Looper.myLooper() == Looper.getMainLooper())
        val texto = leerAsset("texto_crudo_$numero.txt")
        val inicio = System.nanoTime()
        val extraido: TicketExtraido = try {
            textEngine.extract(texto)
        } catch (e: ModeloLocalNoDisponibleException) {
            Assume.assumeTrue("Modelo local no descargado: ${e.message}", false)
            return@runBlocking
        }
        val totalMs = (System.nanoTime() - inicio) / 1_000_000
        assertEquals(Motor.LOCAL, extraido.motor)
        assertTrue("categoria '${extraido.categoria}' fuera de la enum", extraido.categoria in Categoria.entries)
        val acierto = extraido.total?.let { abs(it - totalPapel) <= TOLERANCIA } ?: false
        Log.i(
            TAG,
            "ticket=$numero extraccion=<<<$extraido>>> totalMs=$totalMs " +
                "totalPapel=$totalPapel totalExtraido=${extraido.total} acierto=$acierto",
        )
    }

    private fun leerAsset(nombre: String): String = try {
        InstrumentationRegistry.getInstrumentation().context.assets
            .open(nombre).use { it.bufferedReader(Charsets.UTF_8).readText() }
    } catch (e: IOException) {
        throw AssertionError("falta el asset '$nombre' en app/src/androidTest/assets", e)
    }

    private companion object {
        const val TAG = "ExtraccionLocal"
        const val TOLERANCIA = 0.01
    }
}
