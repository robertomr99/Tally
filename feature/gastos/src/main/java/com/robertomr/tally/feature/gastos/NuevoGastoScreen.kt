package com.robertomr.tally.feature.gastos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.ErrorGastoManual
import com.robertomr.tally.domain.expenses.entities.FormaPago
import com.robertomr.tally.feature.common.CabeceraPantalla
import com.robertomr.tally.feature.common.ColumnaCentrada
import com.robertomr.tally.feature.common.DockEdicion
import com.robertomr.tally.feature.common.FormularioTicketEditable
import com.robertomr.tally.feature.common.PADDING_PANTALLA
import java.time.LocalDate

/** Texto visible del motivo por el que "Guardar" esta deshabilitado (no solo color). */
internal fun textoRequisito(error: ErrorGastoManual): String = when (error) {
    ErrorGastoManual.TOTAL_NO_POSITIVO -> "Indica un total mayor que 0"
    ErrorGastoManual.FECHA_FUTURA -> "La fecha no puede ser futura"
}

/**
 * Alta de un gasto sin ticket (spec gasto-manual): el mismo papel editable de la
 * revision, sin foto ni motor. Las pantallas no reciben el `NavController`:
 * [onBack] vuelve a Gastos sin guardar (ni preguntar); [onGastoGuardado] se
 * invoca una vez al guardar, con la fecha del gasto.
 */
@Composable
fun NuevoGastoScreen(
    onBack: () -> Unit,
    onGastoGuardado: (LocalDate) -> Unit,
    viewModel: NuevoGastoViewModel = hiltViewModel(),
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val estado = rememberNuevoGastoState()

    NuevoGastoContenido(
        ui = ui,
        estado = estado,
        onBack = onBack,
        onGastoGuardado = onGastoGuardado,
        onComercioEditado = viewModel::onComercioEditado,
        onFechaEditada = viewModel::onFechaEditada,
        onTotalEditado = viewModel::onTotalEditado,
        onCategoriaElegida = viewModel::onCategoriaElegida,
        onLineaAnadida = viewModel::onLineaAnadida,
        onLineaConceptoEditado = viewModel::onLineaConceptoEditado,
        onLineaCantidadEditada = viewModel::onLineaCantidadEditada,
        onLineaPrecioEditado = viewModel::onLineaPrecioEditado,
        onLineaEliminada = viewModel::onLineaEliminada,
        onFormaPagoElegida = viewModel::onFormaPagoElegida,
        onEntregadoEditado = viewModel::onEntregadoEditado,
        onCambioEditado = viewModel::onCambioEditado,
        onDescartar = {
            viewModel.onDescartar()
            estado.recrearFormulario()
        },
        onGuardar = viewModel::onGuardar,
    )
}

@Composable
internal fun NuevoGastoContenido(
    ui: NuevoGastoUiState,
    estado: NuevoGastoState,
    onBack: () -> Unit,
    onGastoGuardado: (LocalDate) -> Unit,
    onComercioEditado: (String) -> Unit,
    onFechaEditada: (String) -> Unit,
    onTotalEditado: (String) -> Unit,
    onCategoriaElegida: (Categoria) -> Unit,
    onLineaAnadida: () -> Unit,
    onLineaConceptoEditado: (Long, String) -> Unit,
    onLineaCantidadEditada: (Long, String) -> Unit,
    onLineaPrecioEditado: (Long, String) -> Unit,
    onLineaEliminada: (Long) -> Unit,
    onFormaPagoElegida: (FormaPago) -> Unit,
    onEntregadoEditado: (String) -> Unit,
    onCambioEditado: (String) -> Unit,
    onDescartar: () -> Unit,
    onGuardar: () -> Unit,
) {
    // Guardado correcto: se vuelve a Gastos UNA vez.
    LaunchedEffect(ui.hecho) {
        if (ui.hecho) onGastoGuardado(ui.datos.fechaEmision.toLocalDate())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .imePadding()
            .testTag("pantalla_nuevo_gasto"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ColumnaCentrada {
            CabeceraPantalla(
                titulo = "Nuevo gasto",
                onBack = onBack,
                backTag = "nuevo_gasto_atras",
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(estado.scroll),
        ) {
            ColumnaCentrada {
                Column(
                    modifier = Modifier.padding(horizontal = PADDING_PANTALLA),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    key(estado.versionFormulario) {
                        FormularioTicketEditable(
                            datos = ui.datos,
                            onComercioEditado = onComercioEditado,
                            onFechaEditada = onFechaEditada,
                            onTotalEditado = onTotalEditado,
                            onCategoriaElegida = onCategoriaElegida,
                            onLineaAnadida = onLineaAnadida,
                            onLineaConceptoEditado = onLineaConceptoEditado,
                            onLineaCantidadEditada = onLineaCantidadEditada,
                            onLineaPrecioEditado = onLineaPrecioEditado,
                            onLineaEliminada = onLineaEliminada,
                            onFormaPagoElegida = onFormaPagoElegida,
                            onEntregadoEditado = onEntregadoEditado,
                            onCambioEditado = onCambioEditado,
                        )
                    }
                }
            }
        }

        if (ui.mensajeError != null) {
            MensajeEstado(
                texto = ui.mensajeError,
                icono = Icons.Outlined.ErrorOutline,
                color = MaterialTheme.colorScheme.error,
                tag = "nuevo_gasto_error",
            )
        } else if (ui.error != null) {
            // Icono + texto: el motivo no depende solo del color.
            MensajeEstado(
                texto = textoRequisito(ui.error),
                icono = Icons.Outlined.Info,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                tag = "nuevo_gasto_requisito",
            )
        }

        DockEdicion(
            lineas = ui.datos.lineas,
            onDescartar = onDescartar,
            onGuardar = onGuardar,
            guardarHabilitado = ui.error == null && !ui.guardando && !ui.hecho,
            descartarHabilitado = ui.hayCambios && !ui.guardando && !ui.hecho,
        )
    }
}

/** Fila icono + texto junto al dock (columna centrada de 720 dp). */
@Composable
private fun MensajeEstado(
    texto: String,
    icono: ImageVector,
    color: Color,
    tag: String,
) {
    ColumnaCentrada {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PADDING_PANTALLA, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = texto,
                style = MaterialTheme.typography.bodySmall,
                color = color,
                modifier = Modifier.testTag(tag),
            )
        }
    }
}
