package com.robertomr.tally.feature.ajustes

import com.robertomr.tally.domain.expenses.entities.ModeloLocal
import org.junit.Assert.assertEquals
import org.junit.Test

/** Formato de tamanos de Ajustes (spec descarga-modelo): coma decimal y GB/MB. */
class FormatoTamanoTest {

    @Test
    fun elModeloCompletoSonTresComaCuatroGb() {
        assertEquals("3,4 GB", formatearTamano(ModeloLocal.TAMANO_ESPERADO_BYTES))
    }

    @Test
    fun porDebajoDeUnGbSeUsaMbEntero() {
        assertEquals("50 MB", formatearTamano(52_428_800L))
        assertEquals("0 MB", formatearTamano(0L))
    }

    @Test
    fun elProgresoUsaLaUnidadDelTotalCuandoYaSuperaUnGb() {
        assertEquals("1,2 / 3,4 GB", formatearProgreso(1_288_490_189L, ModeloLocal.TAMANO_ESPERADO_BYTES))
        assertEquals("512 MB / 3,4 GB", formatearProgreso(536_870_912L, ModeloLocal.TAMANO_ESPERADO_BYTES))
    }
}
