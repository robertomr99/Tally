package com.robertomr.tally.feature.gastos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.robertomr.tally.domain.expenses.entities.FiltroFormaPago
import com.robertomr.tally.domain.expenses.entities.PeriodoTipo
import com.robertomr.tally.feature.common.FiltroFormaPagoSelector
import com.robertomr.tally.feature.common.CabeceraDia
import com.robertomr.tally.feature.common.CabeceraPantalla
import com.robertomr.tally.feature.common.CargandoPantalla
import com.robertomr.tally.feature.common.EstadoPantallaDesplazable
import com.robertomr.tally.feature.common.ColumnaCentrada
import com.robertomr.tally.feature.common.FilaTicket
import com.robertomr.tally.feature.common.PADDING_PANTALLA
import com.robertomr.tally.feature.common.PeriodoSelector
import com.robertomr.tally.feature.common.UMBRAL_APILAR_BOTONES
import java.time.LocalDate
import com.robertomr.tally.feature.common.etiquetaPeriodo
import com.robertomr.tally.feature.common.formaFilaTicket

/**
 * Pestaña Gastos (spec listado-gastos): tickets del período agrupados por día.
 * Las pantallas no reciben el NavController: solo callbacks.
 */
@Composable
fun GastosScreen(
    onTicketClick: (Long) -> Unit = {},
    onEscanear: () -> Unit = {},
    onAnadirGasto: () -> Unit = {},
    viewModel: GastosViewModel = hiltViewModel(),
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    GastosContenido(
        ui = ui,
        onSeleccionarPeriodo = viewModel::onSeleccionarPeriodo,
        onSeleccionarRango = viewModel::onSeleccionarRango,
        onTicketClick = onTicketClick,
        onEscanear = onEscanear,
        onAnadirGasto = onAnadirGasto,
        onSeleccionarFiltro = viewModel::onSeleccionarFiltro,
    )
}

/**
 * Contenido sin ViewModel: público para poder pintarlo con estados fijos en los
 * tests instrumentados (p. ej. el vacío, que no se puede forzar con datos reales).
 */
@Composable
fun GastosContenido(
    ui: GastosUiState,
    onSeleccionarPeriodo: (PeriodoTipo) -> Unit,
    onSeleccionarRango: (LocalDate, LocalDate) -> Unit,
    onTicketClick: (Long) -> Unit,
    onEscanear: () -> Unit,
    onAnadirGasto: () -> Unit = {},
    onSeleccionarFiltro: (FiltroFormaPago) -> Unit = {},
) {
    val estado = rememberGastosState()
    ColumnaCentrada(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .testTag("pantalla_gastos"),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Cabecera fija (no desplazable); la lista ocupa el resto con weight.
            // Boton extendido "Añadir gasto" (spec gasto-manual-2, C): en la cabecera con
            // letra normal; con letra grande, en fila propia a ancho completo bajo ella.
            val botonEnFila = LocalDensity.current.fontScale > UMBRAL_APILAR_BOTONES
            CabeceraPantalla(
                titulo = "Gastos",
                subtitulo = ui.periodo?.let { etiquetaPeriodo(it, ui.hoy) },
                subtituloTag = "gastos_rango",
                accion = if (botonEnFila) null else {
                    { BotonAnadirGasto(onAnadirGasto) }
                },
            )
            if (botonEnFila) {
                BotonAnadirGasto(
                    onClick = onAnadirGasto,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PADDING_PANTALLA),
                )
            }
            val grupos = ui.grupos
            val selectores: @Composable () -> Unit = {
                PeriodoSelector(
                    seleccionado = ui.tipo,
                    rango = ui.rango,
                    hoy = ui.hoy,
                    onSeleccionarTipo = onSeleccionarPeriodo,
                    onSeleccionarRango = onSeleccionarRango,
                    modifier = Modifier.padding(horizontal = PADDING_PANTALLA, vertical = 12.dp),
                )
                FiltroFormaPagoSelector(
                    seleccionado = ui.filtro,
                    onSeleccionar = onSeleccionarFiltro,
                    modifier = Modifier.padding(start = PADDING_PANTALLA, end = PADDING_PANTALLA, bottom = 8.dp),
                )
            }
            // Con letra grande, cabecera + selectores ocupan casi toda la pantalla y el vacío
            // quedaría bajo el pliegue: en esos estados los selectores van dentro del mismo scroll.
            val vacioEnScroll = grupos != null && grupos.isEmpty() &&
                LocalDensity.current.fontScale > UMBRAL_APILAR_BOTONES
            if (vacioEnScroll) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    selectores()
                    if (ui.hayTicketsEnPeriodo) {
                        EstadoVacioFiltro(ui.filtro, desplazable = false) { onSeleccionarFiltro(FiltroFormaPago.TODOS) }
                    } else {
                        EstadoVacio(onEscanear, onAnadirGasto, desplazable = false)
                    }
                }
                return@Column
            }
            selectores()
            when {
                // Cargando: cabecera y selector con el indicador común de carga.
                grupos == null -> CargandoPantalla(
                    modifier = Modifier.weight(1f),
                    indicadorModifier = Modifier.testTag("listado_cargando"),
                )
                grupos.isEmpty() && ui.hayTicketsEnPeriodo ->
                    EstadoVacioFiltro(ui.filtro) { onSeleccionarFiltro(FiltroFormaPago.TODOS) }
                grupos.isEmpty() -> EstadoVacio(onEscanear, onAnadirGasto)
                else -> LazyColumn(
                    state = estado.lista,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("lista_gastos"),
                    contentPadding = PaddingValues(start = PADDING_PANTALLA, end = PADDING_PANTALLA, bottom = 24.dp),
                ) {
                    for (grupo in grupos) {
                        item(key = "d_${grupo.fecha}", contentType = "dia") {
                            CabeceraDia(
                                fecha = grupo.fecha,
                                hoy = ui.hoy,
                                totalCentimos = grupo.totalCentimos,
                                tag = "grupo_dia_${grupo.fecha}",
                            )
                        }
                        itemsIndexed(
                            items = grupo.tickets,
                            key = { _, ticket -> "t_${ticket.id}" },
                            contentType = { _, _ -> "ticket" },
                        ) { indice, ticket ->
                            FilaTicket(
                                ticket = ticket,
                                tag = "ticket_${ticket.id}",
                                onClick = onTicketClick,
                                forma = formaFilaTicket(indice == 0, indice == grupo.tickets.lastIndex),
                                conSeparador = indice > 0,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Acción principal de la cabecera: tonal, píldora, icono + texto (no un "+" suelto). */
@Composable
private fun BotonAnadirGasto(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 48.dp)
            .testTag("gastos_anadir"),
        shape = RoundedCornerShape(9999.dp),
        contentPadding = PaddingValues(start = 14.dp, end = 18.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "Añadir gasto", style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

/** Hay tickets en el periodo pero ninguno cumple el filtro: se ofrece volver a "Todos". */
@Composable
private fun EstadoVacioFiltro(
    filtro: FiltroFormaPago,
    desplazable: Boolean = true,
    onVerTodos: () -> Unit,
) {
    EstadoPantallaDesplazable(
        titulo = when (filtro) {
            FiltroFormaPago.EFECTIVO -> "Sin gastos en efectivo en este periodo"
            FiltroFormaPago.TARJETA -> "Sin gastos con tarjeta en este periodo"
            FiltroFormaPago.TODOS -> "Sin gastos en este periodo"
        },
        modifier = (if (desplazable) Modifier.fillMaxSize() else Modifier).testTag("listado_vacio_filtro"),
        icono = Icons.Outlined.ReceiptLong,
        mensaje = "Hay gastos en este periodo con otra forma de pago o sin indicar.",
        desplazable = desplazable,
    ) {
        OutlinedButton(
            onClick = onVerTodos,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("filtro_vaciar"),
        ) {
            Text("Ver todos")
        }
    }
}

@Composable
private fun EstadoVacio(onEscanear: () -> Unit, onAnadirGasto: () -> Unit, desplazable: Boolean = true) {
    // Con letra grande el contenido no cabe en pantalla: se desplaza y, si cabe, queda centrado
    // (alto mínimo = alto disponible).
    EstadoPantallaDesplazable(
        titulo = "Aún no hay tickets en este periodo",
        modifier = (if (desplazable) Modifier.fillMaxSize() else Modifier).testTag("listado_vacio"),
        icono = Icons.Outlined.ReceiptLong,
        mensaje = "Escanea un ticket o añade un gasto a mano y aparecerá aquí.",
        desplazable = desplazable,
    ) {
        // Apilados a ancho completo: caben a 360 dp y con letra grande.
        Button(
            onClick = onEscanear,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("boton_escanear_vacio"),
        ) {
            Text("Escanear ticket")
        }
        OutlinedButton(
            onClick = onAnadirGasto,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("gastos_anadir_vacio"),
        ) {
            Text("Añadir gasto manualmente", textAlign = TextAlign.Center)
        }
    }
}
