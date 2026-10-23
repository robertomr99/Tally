package com.robertomr.tally.feature.ajustes.demo

import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.FormaPago
import com.robertomr.tally.domain.expenses.entities.LineaEditable
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.Periodo
import com.robertomr.tally.domain.expenses.entities.PeriodoTipo
import com.robertomr.tally.domain.expenses.entities.esManual
import com.robertomr.tally.domain.expenses.entities.totalDeLineas
import java.time.LocalDate
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests JVM del generador puro de datos de demo (spec seed-demo, tarea 2). */
class GeneradorDatosDemoTest {

    private val hoy = LocalDate.of(2026, 10, 22) // jueves

    private val conjunto = generarTicketsDemo(hoy)

    @Test
    fun tieneTreceTicketsConClavesUnicasYMarcaEnNota() {
        assertEquals(13, conjunto.size)
        val notas = conjunto.map { it.nota }
        assertEquals(13, notas.toSet().size)
        assertTrue(notas.all { it != null && it.startsWith("tally-demo:v1:D") })
        assertEquals(listOf("D01", "D02", "D03", "D04", "D05", "D06", "D07", "D08", "D09", "D10", "D11", "D12", "D13"),
            notas.map { it!!.removePrefix(MARCA_DEMO) })
        assertTrue(conjunto.all { it.esDemo() })
    }

    @Test
    fun cubreCategoriasFormasDePagoYMotores() {
        assertEquals(Categoria.entries.toSet(), conjunto.map { it.categoria }.toSet())
        assertEquals(FormaPago.entries.toSet(), conjunto.map { it.formaPago }.toSet())
        assertTrue(conjunto.any { it.motor == Motor.LOCAL })
        assertTrue(conjunto.any { it.motor == Motor.NUBE })
        assertTrue(conjunto.any { it.lineas.isNotEmpty() })
        assertTrue(conjunto.any { it.lineas.isEmpty() })
    }

    @Test
    fun hayDosGastosManualesSinMotorNiFoto() {
        val manuales = conjunto.filter { it.esManual }
        assertEquals(2, manuales.size)
        assertTrue(manuales.all { it.motor == null && it.rutaFoto == "" })
    }

    @Test
    fun ningunTicketTieneFotoNiId() {
        assertTrue(conjunto.all { it.rutaFoto == "" && it.id == 0L })
    }

    @Test
    fun conLineasElTotalEsLaSumaDeLineas() {
        conjunto.filter { it.lineas.isNotEmpty() }.forEach { t ->
            val suma = totalDeLineas(
                t.lineas.mapIndexed { i, l -> LineaEditable(i.toLong(), l.concepto, l.cantidad, l.precioUnitario) },
            )
            assertEquals(t.nota, suma, t.total, 1e-9)
        }
    }

    @Test
    fun pagosCoherentes() {
        conjunto.forEach { t ->
            when (t.formaPago) {
                FormaPago.EFECTIVO -> {
                    val entregado = t.entregado
                    if (entregado != null) {
                        assertEquals(t.nota, entregado - t.total, t.cambio!!, 0.005)
                    }
                }
                else -> {
                    assertNull(t.nota, t.entregado)
                    assertNull(t.nota, t.cambio)
                }
            }
        }
        val d02 = conjunto.first { it.nota == marcaDemo("D02") }
        assertEquals(4.00, d02.total, 1e-9)
        assertEquals(5.00, d02.entregado!!, 1e-9)
        assertEquals(1.00, d02.cambio!!, 1e-9)
        val d07 = conjunto.first { it.nota == marcaDemo("D07") }
        assertEquals(6.82, d07.cambio!!, 1e-9)
        val d09 = conjunto.first { it.nota == marcaDemo("D09") }
        assertEquals(32.00, d09.total, 1e-9)
        assertEquals(8.00, d09.cambio!!, 1e-9)
    }

    @Test
    fun ticketMaloEsCorregible() {
        val malo = conjunto.first { it.nota == marcaDemo("D13") }
        assertEquals("Comercio ilegible", malo.comercio)
        assertEquals(0.0, malo.total, 0.0)
        assertEquals(Categoria.OTROS, malo.categoria)
        assertEquals(Motor.NUBE, malo.motor)
        assertTrue(malo.lineas.isEmpty())
        assertEquals("", malo.rutaFoto)
        assertNotNull(malo.textoCrudoOcr)
        assertEquals(FormaPago.NO_INDICADA, malo.formaPago)
    }

    @Test
    fun fechasRelativasAHoyCaenEnSusPeriodos() {
        val semana = Periodo.de(PeriodoTipo.SEMANA, hoy)
        val semanaAnterior = semana.anterior()
        val mesAnterior = Periodo.de(PeriodoTipo.MES, hoy).anterior()
        fun fecha(clave: String) = conjunto.first { it.nota == marcaDemo(clave) }.fechaEmision.toLocalDate()

        assertEquals(hoy, fecha("D01"))
        listOf("D05", "D06", "D07").forEach { c ->
            val f = fecha(c)
            assertTrue("$c $f", !f.isBefore(semanaAnterior.desde) && !f.isAfter(semanaAnterior.hasta))
        }
        listOf("D08", "D09", "D10").forEach { c ->
            val f = fecha(c)
            assertTrue("$c $f", !f.isBefore(mesAnterior.desde) && !f.isAfter(mesAnterior.hasta))
        }
        assertTrue(conjunto.none { it.fechaEmision.toLocalDate().isAfter(hoy) })
        assertTrue(conjunto.all { it.fechaCaptura == it.fechaEmision })
    }

    @Test
    fun hayDatosEnHoySemanaYMes() {
        val dia = Periodo.de(PeriodoTipo.DIA, hoy)
        val semana = Periodo.de(PeriodoTipo.SEMANA, hoy)
        val mes = Periodo.de(PeriodoTipo.MES, hoy)
        fun cuantos(p: Periodo) = conjunto.count {
            val f = it.fechaEmision.toLocalDate(); !f.isBefore(p.desde) && !f.isAfter(p.hasta)
        }
        assertTrue(cuantos(dia) >= 1)
        assertTrue(cuantos(semana) >= 2)
        assertTrue(cuantos(mes) >= 2)
    }

    @Test
    fun otroDiaDaLasMismasClavesYMesesCortosYLunesFuncionan() {
        val claves = conjunto.map { it.nota }
        val dias = listOf(
            LocalDate.of(2026, 3, 1), // mes anterior febrero (28 dias)
            LocalDate.of(2026, 1, 1), // cruce de anio
            LocalDate.of(2026, 10, 19), // lunes
            LocalDate.of(2026, 10, 25), // domingo
            LocalDate.of(2028, 3, 31),
        )
        dias.forEach { d ->
            val otro = generarTicketsDemo(d)
            assertEquals(claves, otro.map { it.nota })
            val mesAnterior = Periodo.de(PeriodoTipo.MES, d).anterior()
            otro.filter { it.nota in listOf(marcaDemo("D08"), marcaDemo("D09"), marcaDemo("D10")) }.forEach {
                val f = it.fechaEmision.toLocalDate()
                assertTrue("$d $f", !f.isBefore(mesAnterior.desde) && !f.isAfter(mesAnterior.hasta))
            }
            assertTrue(abs(otro.sumOf { it.total } - conjunto.sumOf { it.total }) < 1e-9)
        }
    }
}
