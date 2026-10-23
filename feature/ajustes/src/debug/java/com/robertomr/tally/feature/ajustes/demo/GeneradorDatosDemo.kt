package com.robertomr.tally.feature.ajustes.demo

import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.FormaPago
import com.robertomr.tally.domain.expenses.entities.Linea
import com.robertomr.tally.domain.expenses.entities.LineaEditable
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.Ticket
import com.robertomr.tally.domain.expenses.entities.normalizarPago
import com.robertomr.tally.domain.expenses.entities.totalDeLineas
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/*
 * Datos de demo (spec seed-demo). SOLO existe en el source set debug de
 * :feature:ajustes. Kotlin puro: el reloj entra por parametro.
 */

/**
 * Prefijo de la marca de demo, guardada en `Ticket.nota` (siempre `null` en los
 * tickets reales). Es lo UNICO que distingue un ticket de demo: "Quitar" solo
 * borra tickets cuya nota empieza por este prefijo.
 */
internal const val MARCA_DEMO = "tally-demo:v1:"

/** Marca completa de la clave `D01`..`D13`. */
internal fun marcaDemo(clave: String): String = MARCA_DEMO + clave

/** `true` si el ticket lleva la marca de demo. */
internal fun Ticket.esDemo(): Boolean = nota?.startsWith(MARCA_DEMO) == true

private fun linea(concepto: String, cantidad: Int, precio: Double) = Linea(concepto, cantidad, precio)

private fun totalLineas(lineas: List<Linea>): Double =
    totalDeLineas(
        lineas.mapIndexed { i, l -> LineaEditable(i.toLong(), l.concepto, l.cantidad, l.precioUnitario) },
    )

/**
 * Conjunto de tickets de demo relativos a [hoy]: las claves (`nota`) son
 * estables; las fechas no. Todos los ids son 0 (se asignan al guardar), sin
 * foto (`rutaFoto = ""`) y con `fechaCaptura = fechaEmision`.
 *
 * Los pagos pasan por [normalizarPago]; con lineas el total es la suma de lineas.
 */
internal fun generarTicketsDemo(hoy: LocalDate): List<Ticket> {
    val lunes = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val mesAnterior = YearMonth.from(hoy).minusMonths(1)

    fun demo(
        clave: String,
        comercio: String,
        categoria: Categoria,
        dia: LocalDate,
        hora: LocalTime,
        formaPago: FormaPago,
        motor: Motor?,
        lineas: List<Linea> = emptyList(),
        totalSinLineas: Double = 0.0,
        entregado: Double? = null,
        textoCrudoOcr: String? = null,
    ): Ticket {
        val total = if (lineas.isEmpty()) totalSinLineas else totalLineas(lineas)
        val pago = normalizarPago(formaPago, total, entregado, null)
        val fecha: LocalDateTime = dia.atTime(hora)
        return Ticket(
            comercio = comercio,
            fechaEmision = fecha,
            total = total,
            categoria = categoria,
            fechaCaptura = fecha,
            motor = motor,
            rutaFoto = "",
            nota = marcaDemo(clave),
            textoCrudoOcr = textoCrudoOcr,
            lineas = lineas,
            formaPago = pago.formaPago,
            entregado = pago.entregado,
            cambio = pago.cambio,
        )
    }

    return listOf(
        demo(
            "D01", "Super Barrio", Categoria.ALIMENTACION, hoy, LocalTime.of(9, 5),
            FormaPago.TARJETA, Motor.NUBE,
            lineas = listOf(
                linea("Leche entera", 2, 0.95),
                linea("Pan de molde", 1, 1.15),
                linea("Tomates", 1, 2.80),
                linea("Pechuga de pollo", 1, 6.45),
            ),
        ),
        demo(
            "D02", "Café La Esquina", Categoria.ALIMENTACION, hoy.minusDays(1), LocalTime.of(8, 40),
            FormaPago.EFECTIVO, Motor.LOCAL,
            lineas = listOf(linea("Café", 1, 1.60), linea("Tostada", 1, 2.40)),
            entregado = 5.00,
        ),
        demo(
            "D03", "Gasolinera Norte", Categoria.TRANSPORTE, hoy.minusDays(2), LocalTime.of(18, 20),
            FormaPago.TARJETA, Motor.NUBE, totalSinLineas = 52.30,
        ),
        demo(
            "D04", "Farmacia Central", Categoria.SALUD, hoy.minusDays(3), LocalTime.of(11, 10),
            FormaPago.EFECTIVO, Motor.LOCAL,
            lineas = listOf(linea("Ibuprofeno", 1, 8.45), linea("Crema hidratante", 1, 12.90)),
            entregado = 30.00,
        ),
        demo(
            "D05", "Cine Plaza", Categoria.OCIO, lunes.minusDays(6), LocalTime.of(21, 0),
            FormaPago.TARJETA, Motor.NUBE, totalSinLineas = 17.50,
        ),
        demo(
            "D06", "Ferretería Hogar", Categoria.HOGAR, lunes.minusDays(4), LocalTime.of(17, 30),
            FormaPago.NO_INDICADA, Motor.NUBE,
            lineas = listOf(
                linea("Bombilla LED", 2, 3.95),
                linea("Cinta aislante", 1, 4.50),
                linea("Tornillos", 1, 2.85),
            ),
        ),
        demo(
            "D07", "Supermercado Sur", Categoria.ALIMENTACION, lunes.minusDays(3), LocalTime.of(19, 15),
            FormaPago.EFECTIVO, Motor.LOCAL, totalSinLineas = 63.18, entregado = 70.00,
        ),
        demo(
            "D08", "Taxi Ciudad", Categoria.TRANSPORTE, mesAnterior.atDay(12), LocalTime.of(23, 10),
            FormaPago.TARJETA, Motor.NUBE, totalSinLineas = 14.60,
        ),
        demo(
            "D09", "Restaurante El Parque", Categoria.OCIO, mesAnterior.atDay(20), LocalTime.of(14, 25),
            FormaPago.EFECTIVO, Motor.LOCAL,
            lineas = listOf(
                linea("Menú del día", 2, 12.50),
                linea("Bebida", 2, 2.20),
                linea("Café", 2, 1.30),
            ),
            entregado = 40.00,
        ),
        demo(
            "D10", "Clínica Dental", Categoria.SALUD, mesAnterior.atDay(3), LocalTime.of(10, 0),
            FormaPago.NO_INDICADA, Motor.NUBE, totalSinLineas = 45.00,
        ),
        demo(
            "D11", "Trastero mensual", Categoria.HOGAR, hoy.minusDays(4), LocalTime.of(12, 0),
            FormaPago.EFECTIVO, null, totalSinLineas = 35.00, entregado = 35.00,
        ),
        demo(
            "D12", "Propina", Categoria.OTROS, hoy.minusDays(1), LocalTime.of(14, 0),
            FormaPago.NO_INDICADA, null, totalSinLineas = 5.00,
        ),
        // Ticket "malo": para demostrar la correccion manual en Detalle > Editar.
        demo(
            "D13", "Comercio ilegible", Categoria.OTROS, hoy.minusDays(1), LocalTime.of(16, 45),
            FormaPago.NO_INDICADA, Motor.NUBE, totalSinLineas = 0.0,
            textoCrudoOcr = "T1ENd@ ?? 0 ### 1?.4O\n  TOT4L ..... ?? ,,\n§§ gR@c1@s ## v|ue1v@ ??",
        ),
    )
}
