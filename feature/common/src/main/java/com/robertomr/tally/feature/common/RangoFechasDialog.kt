package com.robertomr.tally.feature.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.robertomr.tally.domain.expenses.entities.Periodo
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    // Meses y días de la semana en español aunque el dispositivo esté en otro idioma:
    // el picker toma el idioma de la configuración de Compose.
    val configuracion = Configuration(LocalConfiguration.current).apply { setLocale(LOCALE_ES) }
    CompositionLocalProvider(LocalConfiguration provides configuracion) {
        DialogoRango(hoy, inicial, onAplicar, onCancelar)
    }
}

@Composable
private fun DialogoRango(
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
        dismissButton = {
            TextButton(onClick = onCancelar, modifier = Modifier.testTag("rango_cancelar")) { Text("Cancelar") }
        },
    ) {
        DateRangePicker(
            state = estado,
            modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp).padding(top = 4.dp),
            title = { Text("Seleccionar rango de fechas", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) },
            headline = { CabeceraRango(inicio, fin) },
        )
    }
}

/**
 * Cabecera propia con las dos fechas en una sola línea y en español. La de
 * Material reparte el ancho a partes iguales y apila en vertical la fecha final
 * ("Oct, 19, 2026") cuando no cabe; aquí cada fecha ocupa una línea y se acorta.
 */
@Composable
private fun CabeceraRango(inicio: Long?, fin: Long?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Sin pesos: con weight(1f) la fecha inicial ocupaba medio ancho y el guion quedaba lejos de ella.
        TextoFecha(inicio, "Fecha inicial", Modifier.testTag("rango_cabecera_inicio"))
        Text("–", style = MaterialTheme.typography.titleMedium)
        TextoFecha(fin, "Fecha final", Modifier.testTag("rango_cabecera_fin"))
    }
}

@Composable
private fun TextoFecha(millis: Long?, vacio: String, modifier: Modifier) {
    val texto = millis?.aLocalDateUtc()?.format(FORMATO_CABECERA) ?: vacio
    Text(
        text = texto,
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        color = if (millis == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

private val LOCALE_ES: Locale = Locale.forLanguageTag("es-ES")
private val FORMATO_CABECERA: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", LOCALE_ES)

/** Solo días hasta hoy inclusive; sin límite inferior. */
private class HastaHoy(private val hoy: LocalDate) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean = !utcTimeMillis.aLocalDateUtc().isAfter(hoy)
    override fun isSelectableYear(year: Int): Boolean = year <= hoy.year
}
