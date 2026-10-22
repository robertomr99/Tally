package com.robertomr.tally.feature.gastos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.robertomr.tally.feature.common.EstadoPantalla
import com.robertomr.tally.feature.common.ColumnaCentrada
import com.robertomr.tally.feature.common.FilaTicket
import com.robertomr.tally.feature.common.PADDING_PANTALLA
import com.robertomr.tally.feature.common.PeriodoSelector
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
            CabeceraPantalla(
                titulo = "Gastos",
                subtitulo = ui.periodo?.let { etiquetaPeriodo(it, ui.hoy) },
                subtituloTag = "gastos_rango",
                accion = {
                    IconButton(
                        onClick = onAnadirGasto,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("gastos_anadir"),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "Añadir gasto",
                        )
                    }
                },
            )
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

            val grupos = ui.grupos
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

/** Hay tickets en el periodo pero ninguno cumple el filtro: se ofrece volver a "Todos". */
@Composable
private fun EstadoVacioFiltro(filtro: FiltroFormaPago, onVerTodos: () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize().testTag("listado_vacio_filtro")) {
        val altoDisponible = maxHeight
        EstadoPantalla(
            titulo = when (filtro) {
                FiltroFormaPago.EFECTIVO -> "Sin gastos en efectivo en este periodo"
                FiltroFormaPago.TARJETA -> "Sin gastos con tarjeta en este periodo"
                FiltroFormaPago.TODOS -> "Sin gastos en este periodo"
            },
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = altoDisponible),
            icono = Icons.Outlined.ReceiptLong,
            mensaje = "Hay gastos en este periodo con otra forma de pago o sin indicar.",
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
}

@Composable
private fun EstadoVacio(onEscanear: () -> Unit, onAnadirGasto: () -> Unit) {
    // Con letra grande el contenido no cabe en pantalla: se desplaza y, si cabe, queda centrado
    // (alto mínimo = alto disponible).
    BoxWithConstraints(modifier = Modifier.fillMaxSize().testTag("listado_vacio")) {
        val altoDisponible = maxHeight
        EstadoPantalla(
            titulo = "Aún no hay tickets en este periodo",
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = altoDisponible),
            icono = Icons.Outlined.ReceiptLong,
            mensaje = "Escanea un ticket o añade un gasto a mano y aparecerá aquí.",
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
}
