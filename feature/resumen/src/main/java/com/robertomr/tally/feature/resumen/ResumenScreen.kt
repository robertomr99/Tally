package com.robertomr.tally.feature.resumen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.robertomr.tally.domain.expenses.entities.PeriodoTipo

/**
 * Pestaña Resumen: selector de período y tarjeta de total con sus métricas.
 */
@Composable
fun ResumenScreen(
    onAbrirAjustes: () -> Unit = {},
    viewModel: ResumenViewModel = hiltViewModel(),
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    ResumenContenido(
        ui = ui,
        onSeleccionarPeriodo = viewModel::onSeleccionarPeriodo,
        onAbrirAjustes = onAbrirAjustes,
    )
}

@Composable
private fun ResumenContenido(
    ui: ResumenUiState,
    onSeleccionarPeriodo: (PeriodoTipo) -> Unit,
    onAbrirAjustes: () -> Unit,
) {
    val estado = rememberResumenState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .verticalScroll(estado.scroll)
            .testTag("pantalla_resumen"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Tally",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            // Entrada a Ajustes (spec descarga-modelo, D2).
            IconButton(onClick = onAbrirAjustes, modifier = Modifier.testTag("boton_ajustes")) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Ajustes",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        PeriodoSelector(
            seleccionado = ui.tipo,
            onSeleccionar = onSeleccionarPeriodo,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TarjetaTotal(periodo = ui.periodo, resumen = ui.resumen)
        }
    }
}
