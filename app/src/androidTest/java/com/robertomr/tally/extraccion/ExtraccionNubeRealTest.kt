package com.robertomr.tally.extraccion

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.debug.InternalDebugSecretProvider
import com.google.firebase.appcheck.debug.internal.DebugAppCheckProvider
import com.google.firebase.inject.Provider
import com.robertomr.tally.di.TextEngineEntryPoint
import com.robertomr.tally.domain.expenses.data.TextEngine
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.TicketExtraido
import dagger.hilt.android.EntryPointAccessors
import java.io.IOException
import java.util.concurrent.Executors
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test REAL de la extraccion en nube (spec extraccion-nube, T6/T7 = verificacion
 * 1): corre el [TextEngine] de Firebase AI Logic (Gemini) sobre los **4 textos
 * crudos reales** de `docs/evidence/ocr-mlkit/texto_crudo_1..4.txt` (copiados a
 * `app/src/androidTest/assets/`, bytes identicos) y comprueba que cada salida
 * pasa el parser/validador compartido (schema + enum de 6) con los totales
 * esperados.
 *
 * - Ticket 1 (HOLA ALMIRANTE): la foto trae DOS bloques y el texto crudo ambos
 *   importes ("Total (impuestos incl.) 32,09" y "Total: 35,30"). CONTRASTE
 *   HUMANO CON EL PAPEL (decision del usuario 2026-10-17, R3/U4: el papel manda
 *   sobre el test): el total CON IVA real es **35,30**; la expectativa 32,09
 *   del test estaba mal derivada del OCR.
 * - Ticket 2 es el caso TRAMPA de R3: "3,36 IVA: 0,14" + "TOTAL COMPRA 3,50" →
 *   el total debe ser 3.50 (con IVA), nunca 3.36 (base).
 * - Ticket 3 (OZONE): CONTRASTE HUMANO CON EL PAPEL (2026-10-17, R3/U4): el
 *   total CON IVA real es **13,90** (50,00 − 36,10 de cambio); el "Total
 *   (impuestos incl.) 12,64" del OCR era engañoso. Antes solo se registraba la
 *   extracción por la ambigüedad 12,64/13,90; con la decisión humana tomada se
 *   exige el total 13,90.
 * - Sin `app/google-services.json` (prerrequisito humano U1) el test se SALTA
 *   limpiamente con `assumeTrue` y motivo explicito (patrón
 *   `MlKitOcrEngineTest`): no se inventan resultados de nube.
 *
 * Inyeccion via [EntryPointAccessors] sobre la Application real
 * (`TallyApplication`, `@HiltAndroidApp`): desviacion documentada del spec
 * (R-NUBE6) - `HiltTestApplication` exige `HiltAndroidRule` en TODOS los tests
 * instrumentados y rompia `NavigationTest`/`CapturaFlujoTest`; el propio spec
 * preveia esta alternativa. El runner queda en `AndroidJUnitRunner` estandar.
 *
 * Las extracciones reales se loguean con el tag `ExtraccionNube` para exportarlas
 * a `docs/evidence/extraccion-nube/` cuando corra con credenciales (T7).
 */
@RunWith(AndroidJUnit4::class)
class ExtraccionNubeRealTest {

    private val textEngine: TextEngine by lazy {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        EntryPointAccessors
            .fromApplication(contexto, TextEngineEntryPoint::class.java)
            .textEngine()
    }

    @Before
    fun skipSiFirebaseNoEstaConfigurado() {
        // google_app_id es un recurso que SOLO inyecta el plugin google-services
        // al aplicarse con app/google-services.json (T1 condicional).
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val googleAppId = contexto.resources
            .getIdentifier("google_app_id", "string", contexto.packageName)
        Assume.assumeTrue(
            "Firebase AI no configurado (falta app/google-services.json, prerrequisito U1 " +
                "del spec extraccion-nube): test real de nube omitido, sin inventar resultados.",
            googleAppId != 0,
        )

        // App Check debug provider (solo lo instala este test; la libreria vive en
        // androidTest + variante debug de :app): Firebase AI Logic exige App
        // Check enforced (obligatorio desde 2026-11-02). Sin provider de produccion
        // (pendiente futuro). El debug token hay que registrarlo a mano en la consola
        // (Security > App Check > Apps > Manage debug tokens).
        instalarProveedorAppCheck()
    }

    /**
     * Instala el provider de debug de App Check.
     *
     * Si el harness ha inyectado un token fijo (argumento [ARG_TOKEN_APP_CHECK],
     * leido por :app desde `app/appcheck-debug-token.txt`, fuera de VCS) se usa ese
     * token: es el registrado a mano en la consola y asi la ejecucion es
     * determinista. Sin token fijo se recurre al factory estandar, que genera un
     * token NUEVO por instalacion: como AGP desinstala la app al terminar cada
     * `connectedDebugAndroidTest`, ese token no sobrevive a la siguiente ejecucion
     * (verificado 2026-10-17: tres ejecuciones, tres tokens distintos) y habria
     * que registrarlo de nuevo cada vez.
     */
    private fun instalarProveedorAppCheck() {
        val tokenFijo = InstrumentationRegistry.getArguments()
            .getString(ARG_TOKEN_APP_CHECK)
            ?.takeIf { it.isNotBlank() }
        if (tokenFijo == null) {
            FirebaseAppCheck.getInstance()
                .installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
            return
        }
        val app = FirebaseApp.getInstance()
        val proveedor = DebugAppCheckProvider(
            app,
            Provider { InternalDebugSecretProvider { tokenFijo } },
            Executors.newSingleThreadExecutor(),
            Executors.newSingleThreadExecutor(),
            Executors.newSingleThreadExecutor(),
        )
        FirebaseAppCheck.getInstance()
            .installAppCheckProviderFactory { proveedor }
    }

    @Test
    fun theInjectedEngineIsTheCloudOne() {
        assertEquals(Motor.NUBE, textEngine.motor)
    }

    @Test
    fun ticket1ExtractionIsValidWithTotal3530(): Unit = runBlocking {
        // CONTRASTE HUMANO CON EL PAPEL (decision del usuario 2026-10-17, R3/U4:
        // el papel manda sobre el test): el total CON IVA real del ticket 1 es
        // 35,30; el "32,09" del texto crudo ("Total (impuestos incl.)") era el
        // bloque equivocado de los dos que trae la foto. La expectativa 32,09
        // de este test estaba mal derivada del OCR y se corrige (evidencia > test).
        val extraido = extraer(1)

        assertPasaSchemaYEnum(extraido)
        assertEquals("el total con IVA del ticket 1 es 35,30 (papel)", 35.30, extraido.total!!, TOLERANCIA)
    }

    @Test
    fun ticket2ExtractionUsesTheTotalWithVatNotTheBase(): Unit = runBlocking {
        // R3 (deuda de ocr-mlkit): base 3,36 + cuota IVA 0,14 = TOTAL COMPRA 3,50.
        val extraido = extraer(2)

        assertPasaSchemaYEnum(extraido)
        assertEquals("el total CON IVA del ticket 2 es 3,50", 3.50, extraido.total!!, TOLERANCIA)
        assertTrue(
            "el total NO debe ser la base imponible 3,36: fue ${extraido.total}",
            kotlin.math.abs(extraido.total!! - 3.36) > TOLERANCIA,
        )
    }

    @Test
    fun ticket3ExtractionIsValidWithTotal1390(): Unit = runBlocking {
        // CONTRASTE HUMANO CON EL PAPEL (decision del usuario 2026-10-17, R3/U4:
        // el papel manda sobre el test): el total CON IVA real del ticket 3
        // (OZONE) es 13,90 = 50,00 entregado - 36,10 de cambio; el "Total
        // (impuestos incl.) 12,64" del OCR era enganoso. Antes este test solo
        // REGISTRABA la extraccion por la ambiguedad 12,64/13,90; con la
        // decision humana ya tomada se exige el total 13,90 con tolerancia.
        val extraido = extraer(3)

        assertPasaSchemaYEnum(extraido)
        assertEquals("el total con IVA del ticket 3 es 13,90 (papel)", 13.90, extraido.total!!, TOLERANCIA)
    }

    @Test
    fun ticket4ExtractionIsValidWithTotal791(): Unit = runBlocking {
        val extraido = extraer(4)

        assertPasaSchemaYEnum(extraido)
        assertEquals("el total con IVA del ticket 4 es 7,91", 7.91, extraido.total!!, TOLERANCIA)
    }

    private suspend fun extraer(numero: Int): TicketExtraido {
        val textoCrudo = leerAsset("texto_crudo_$numero.txt")
        val extraido = textEngine.extract(textoCrudo)
        Log.i(TAG, "extraccion_$numero=<<<$extraido>>>")
        return extraido
    }

    /**
     * Criterio 1 de la verificacion: la salida pasa el parser/validador
     * compartido (llegar aqui ya es pasar el schema) y la categoria es una de
     * las 6 de la enum fija.
     */
    private fun assertPasaSchemaYEnum(extraido: TicketExtraido) {
        assertTrue(
            "categoria '${extraido.categoria}' fuera de la enum de 6",
            extraido.categoria in Categoria.entries,
        )
        assertEquals(Motor.NUBE, extraido.motor)
        assertTrue(
            "el total extraido no puede ser negativo: ${extraido.total}",
            extraido.total == null || extraido.total!! >= 0.0,
        )
    }

    private fun leerAsset(nombre: String): String = try {
        InstrumentationRegistry.getInstrumentation().context.assets
            .open(nombre).use { it.bufferedReader(Charsets.UTF_8).readText() }
    } catch (e: IOException) {
        throw AssertionError("falta el asset '$nombre' en app/src/androidTest/assets", e)
    }

    private companion object {
        const val TAG = "ExtraccionNube"
        const val TOLERANCIA = 0.01

        /** Argumento del runner con el debug token fijo de App Check (opcional). */
        const val ARG_TOKEN_APP_CHECK = "appcheckDebugToken"
    }
}
