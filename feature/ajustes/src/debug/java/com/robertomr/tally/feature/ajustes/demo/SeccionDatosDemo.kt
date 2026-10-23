package com.robertomr.tally.feature.ajustes.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.robertomr.tally.feature.common.TarjetaSeccion
import com.robertomr.tally.feature.common.TituloSeccion

/** Estado puramente visual de los dialogos de confirmacion (setters privados). */
internal class DatosDemoState {
    var confirmandoSembrar by mutableStateOf(false)
        private set
    var confirmandoQuitar by mutableStateOf(false)
        private set

    fun pedirSembrar() { confirmandoSembrar = true }
    fun cerrarSembrar() { confirmandoSembrar = false }
    fun pedirQuitar() { confirmandoQuitar = true }
    fun cerrarQuitar() { confirmandoQuitar = false }
}

@Composable
internal fun rememberDatosDemoState(): DatosDemoState = remember { DatosDemoState() }

/**
 * Seccion "Datos de demo" de Ajustes (spec seed-demo, D7). SOLO build debug: en
 * release existe un stub vacio con esta misma firma. Los textos no truncan y la
 * columna de Ajustes desplaza, asi que cabe a 360 dp y con fontScale 2,0.
 */
@Composable
internal fun SeccionDatosDemo() {
    val viewModel: DatosDemoViewModel = hiltViewModel()
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val estado = rememberDatosDemoState()
    SeccionDatosDemoContent(
        ui = ui,
        estado = estado,
        onSembrar = viewModel::onSembrar,
        onQuitar = viewModel::onQuitar,
    )
}

@Composable
internal fun SeccionDatosDemoContent(
    ui: DatosDemoUiState,
    estado: DatosDemoState,
    onSembrar: () -> Unit,
    onQuitar: () -> Unit,
) {
    TarjetaSeccion(
        modifier = Modifier.testTag("seccion_datos_demo"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TituloSeccion("Datos de demo")
        Text(
            "Solo en builds de depuración. Crea gastos falsos para la demo; los tickets " +
                "reales no se tocan. Quitar borra los tickets de demo, también los que hayas editado.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Tickets de demo: ${ui.contador}",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.testTag("demo_contador"),
        )
        Button(
            onClick = estado::pedirSembrar,
            enabled = !ui.ocupado,
            modifier = Modifier.fillMaxWidth().testTag("demo_sembrar"),
        ) { Text("Sembrar datos de demo") }
        OutlinedButton(
            onClick = estado::pedirQuitar,
            enabled = !ui.ocupado,
            modifier = Modifier.fillMaxWidth().testTag("demo_quitar"),
        ) { Text("Quitar datos de demo") }
        ui.resultado?.let { texto ->
            Text(
                text = texto,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .testTag("demo_resultado"),
            )
        }
    }

    if (estado.confirmandoSembrar) {
        AlertDialog(
            onDismissRequest = estado::cerrarSembrar,
            title = { Text("Sembrar datos de demo") },
            text = { Text("Se añadirán gastos falsos de varias categorías y períodos. No se tocará ningún ticket real.") },
            confirmButton = {
                Button(
                    onClick = {
                        estado.cerrarSembrar()
                        onSembrar()
                    },
                    modifier = Modifier.testTag("demo_confirmar_sembrar"),
                ) { Text("Sembrar") }
            },
            dismissButton = { TextButton(onClick = estado::cerrarSembrar) { Text("Cancelar") } },
        )
    }

    if (estado.confirmandoQuitar) {
        AlertDialog(
            onDismissRequest = estado::cerrarQuitar,
            title = { Text("Quitar datos de demo") },
            text = { Text("Se borrarán solo los tickets de demo. Los tickets reales se conservan.") },
            confirmButton = {
                Button(
                    onClick = {
                        estado.cerrarQuitar()
                        onQuitar()
                    },
                    modifier = Modifier.testTag("demo_confirmar_quitar"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) { Text("Quitar") }
            },
            dismissButton = { TextButton(onClick = estado::cerrarQuitar) { Text("Cancelar") } },
        )
    }
}
