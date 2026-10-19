package com.robertomr.tally.feature.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.robertomr.tally.domain.expenses.entities.Periodo
import java.time.LocalDate

/**
 * Selector de rango de fechas (mockup 09 reducido, spec rango-personalizado D3):
 * `DatePickerDialog` con `DateRangePicker` de Material 3. Los días posteriores a
 * [hoy] no son seleccionables (D6). "Aplicar rango" queda deshabilitado hasta
 * tener inicio Y fin; el picker ordena la selección, así que no puede salir un
 * rango invertido. [inicial] precarga el último rango elegido.
 */
@Composable
fun RangoFechasDialog(
    hoy: LocalDate,
    inicial: Periodo?,
    onAplicar: (desde: LocalDate, hasta: LocalDate) -> Unit,
    onCancelar: () -> Unit,
) {
    val estado = rememberDateRangePickerState(
        initialSelectedStartDateMillis = inicial?.desde?.aMillisUtc(),
        initialSelectedEndDateMillis = inicial?.hasta?.aMillisUtc(),
        initialDisplayedMonthMillis = (inicial?.hasta ?: hoy).aMillisUtc(),
        selectableDates = remember(hoy) { HastaHoy(hoy) },
    )
    val inicio = estado.selectedStartDateMillis
    val fin = estado.selectedEndDateMillis
    DatePickerDialog(
        onDismissRequest = onCancelar,
        confirmButton = {
            TextButton(
                onClick = {
                    if (inicio != null && fin != null) onAplicar(inicio.aLocalDateUtc(), fin.aLocalDateUtc())
                },
                enabled = inicio != null && fin != null,
                modifier = Modifier.testTag("rango_aplicar"),
            ) { Text("Aplicar rango") }
        },
        modifier = Modifier.testTag("rango_dialogo"),
        // La forma extraLarge del tema es una píldora (9999 dp): el diálogo necesita la suya.
        shape = RoundedCornerShape(28.dp),
        dismissButton = {
            TextButton(onClick = onCancelar, modifier = Modifier.testTag("rango_cancelar")) { Text("Cancelar") }
        },
    ) {
        DateRangePicker(
            state = estado,
            modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp).padding(top = 4.dp),
            title = { Text("Seleccionar rango de fechas", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) },
        )
    }
}

/** Solo días hasta hoy inclusive; sin límite inferior. */
private class HastaHoy(private val hoy: LocalDate) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean = !utcTimeMillis.aLocalDateUtc().isAfter(hoy)
    override fun isSelectableYear(year: Int): Boolean = year <= hoy.year
}
