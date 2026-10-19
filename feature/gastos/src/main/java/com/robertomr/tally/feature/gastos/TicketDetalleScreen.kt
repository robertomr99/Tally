package com.robertomr.tally.feature.gastos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.feature.common.BadgeMotor
import com.robertomr.tally.feature.common.DockEdicion
import com.robertomr.tally.feature.common.FormularioTicketEditable
import com.robertomr.tally.feature.common.FotoOriginal
import com.robertomr.tally.feature.common.TarjetaFotoAusente
import com.robertomr.tally.feature.common.existeFoto
import com.robertomr.tally.feature.common.formatoImporte

/**
 * Detalle de un ticket guardado (spec detalle-ticket, mockups 07/08 adaptados a
 * DESIGN.md): foto original, cabecera y lineas con la MISMA edicion que la
 * revision (formulario compartido de `:feature:common`), "Guardar cambios" /
 * "Descartar" y "Eliminar ticket" con confirmacion.
 *
 * El id del ticket llega por el argumento de la ruta (`SavedStateHandle`). Las
 * pantallas no reciben el `NavController`: [onBack] cierra el detalle (atras,
 * flecha, tras guardar y tras borrar; descartar cambios al volver no pregunta).
 */
@Composable
fun TicketDetalleScreen(
    onBack: () -> Unit,
    viewModel: TicketDetalleViewModel = hiltViewModel(),
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val estado = rememberTicketDetalleState()

    TicketDetalleContenido(
        ui = ui,
        estado = estado,
        onBack = onBack,
        onComercioEditado = viewModel::onComercioEditado,
        onFechaEditada = viewModel::onFechaEditada,
        onTotalEditado = viewModel::onTotalEditado,
        onCategoriaElegida = viewModel::onCategoriaElegida,
        onLineaAnadida = viewModel::onLineaAnadida,
        onLineaConceptoEditado = viewModel::onLineaConceptoEditado,
        onLineaCantidadEditada = viewModel::onLineaCantidadEditada,
        onLineaPrecioEditado = viewModel::onLineaPrecioEditado,
        onLineaEliminada = viewModel::onLineaEliminada,
        onDescartar = {
            viewModel.onDescartar()
            estado.recrearFormulario()
        },
        onGuardar = viewModel::onGuardar,
        onConfirmarBorrado = {
            estado.cerrarConfirmarBorrado()
            viewModel.onConfirmarBorrado()
        },
    )
}

@Composable
internal fun TicketDetalleContenido(
    ui: TicketDetalleUiState,
    estado: TicketDetalleState,
    onBack: () -> Unit,
    onComercioEditado: (String) -> Unit,
    onFechaEditada: (String) -> Unit,
    onTotalEditado: (String) -> Unit,
    onCategoriaElegida: (Categoria) -> Unit,
    onLineaAnadida: () -> Unit,
    onLineaConceptoEditado: (Long, String) -> Unit,
    onLineaCantidadEditada: (Long, String) -> Unit,
    onLineaPrecioEditado: (Long, String) -> Unit,
    onLineaEliminada: (Long) -> Unit,
    onDescartar: () -> Unit,
    onGuardar: () -> Unit,
    onConfirmarBorrado: () -> Unit,
) {
    val datos = ui as? TicketDetalleUiState.Datos

    // Guardado/borrado correcto: se vuelve a la pantalla de origen UNA vez.
    LaunchedEffect(datos?.hecho) {
        if (datos?.hecho == true) onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .testTag("pantalla_detalle_ticket"),
    ) {
        BarraSuperior(
            onBack = onBack,
            onEliminar = estado::abrirConfirmarBorrado,
            eliminarHabilitado = datos != null && !datos.guardando && !datos.borrando && !datos.hecho,
            mostrarEliminar = datos != null,
        )

        when (ui) {
            TicketDetalleUiState.Cargando -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.testTag("detalle_cargando"))
            }

            TicketDetalleUiState.NoExiste -> NoExiste(onBack = onBack)

            is TicketDetalleUiState.Datos -> {
                val ocupado = ui.guardando || ui.borrando || ui.hecho
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("detalle_ticket_${ui.ticket.id}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BadgeMotor(
                        motor = ui.ticket.motor,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(estado.scroll)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        SeccionFoto(
                            rutaFoto = ui.ticket.rutaFoto,
                            onVerFoto = estado::abrirFoto,
                        )
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
                            )
                        }
                    }

                    if (ui.error != null) {
                        Text(
                            text = ui.error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 4.dp)
                                .testTag("detalle_error"),
                        )
                    }

                    DockEdicion(
                        lineas = ui.datos.lineas,
                        onDescartar = onDescartar,
                        onGuardar = onGuardar,
                        guardarHabilitado = ui.hayCambios && !ocupado,
                        descartarHabilitado = ui.hayCambios && !ocupado,
                    )
                }

                if (estado.mostrarFoto) {
                    VisorFoto(
                        rutaFoto = ui.ticket.rutaFoto,
                        onCerrar = estado::cerrarFoto,
                    )
                }
                if (estado.mostrarConfirmarBorrado) {
                    ConfirmarBorrado(
                        comercio = ui.ticket.comercio,
                        total = ui.ticket.total,
                        onCancelar = estado::cerrarConfirmarBorrado,
                        onConfirmar = onConfirmarBorrado,
                    )
                }
            }
        }
    }
}

/** Barra superior: atras, titulo y "Eliminar" en paleta `error` (solo con ticket cargado). */
@Composable
private fun BarraSuperior(
    onBack: () -> Unit,
    onEliminar: () -> Unit,
    eliminarHabilitado: Boolean,
    mostrarEliminar: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .testTag("detalle_atras"),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Volver",
            )
        }
        Text(
            text = "Detalle del ticket",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (mostrarEliminar) {
            IconButton(
                onClick = onEliminar,
                enabled = eliminarHabilitado,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("detalle_eliminar"),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Eliminar ticket",
                )
            }
        }
    }
}

@Composable
private fun NoExiste(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("detalle_no_existe"),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Este ticket ya no existe",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onBack,
            modifier = Modifier
                .height(48.dp)
                .testTag("detalle_volver"),
            shape = RoundedCornerShape(9999.dp),
        ) {
            Text("Volver")
        }
    }
}

/**
 * "Ver foto original" (mockup 07) si el fichero existe; si falta o la ruta esta
 * en blanco, la tarjeta "Foto no disponible" en su lugar (el resto sigue igual).
 */
@Composable
private fun SeccionFoto(rutaFoto: String, onVerFoto: () -> Unit) {
    val context = LocalContext.current
    val disponible by produceState<Boolean?>(initialValue = null, rutaFoto) {
        value = existeFoto(context.filesDir, rutaFoto)
    }
    when (disponible) {
        true -> OutlinedButton(
            onClick = onVerFoto,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("detalle_ver_foto"),
            shape = RoundedCornerShape(9999.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ver foto original")
        }

        false -> TarjetaFotoAusente(modifier = Modifier.testTag("detalle_foto_ausente"))
        null -> Unit
    }
}

/** Visor a pantalla completa (D4): foto ajustada, sin zoom; cierra con la X o atras. */
@Composable
private fun VisorFoto(rutaFoto: String, onCerrar: () -> Unit) {
    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .statusBarsPadding(),
        ) {
            FotoOriginal(
                rutaFoto = rutaFoto,
                testTagImagen = "detalle_foto",
                testTagAusente = "detalle_foto_no_legible",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp, top = 64.dp, bottom = 16.dp),
            )
            IconButton(
                onClick = onCerrar,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(48.dp)
                    .testTag("detalle_cerrar_foto"),
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
            ) {
                Icon(imageVector = Icons.Outlined.Close, contentDescription = "Cerrar foto")
            }
        }
    }
}

/** Dialogo M3 de confirmacion (forma 28 dp del tema): "Eliminar" en paleta `error`. */
@Composable
private fun ConfirmarBorrado(
    comercio: String,
    total: Double,
    onCancelar: () -> Unit,
    onConfirmar: () -> Unit,
) {
    val nombre = comercio.ifBlank { "este ticket" }
    AlertDialog(
        onDismissRequest = onCancelar,
        modifier = Modifier.testTag("detalle_confirmar_borrado"),
        // La forma extraLarge del tema es una píldora: el diálogo lleva su forma de 28 dp.
        shape = RoundedCornerShape(28.dp),
        title = { Text("Eliminar ticket") },
        text = {
            Text(
                "Se eliminará «$nombre» por ${formatoImporte(total)} junto con sus " +
                    "líneas y su foto. Esta acción no se puede deshacer.",
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmar,
                modifier = Modifier.testTag("detalle_confirmar_eliminar"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text("Eliminar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancelar,
                modifier = Modifier.testTag("detalle_cancelar_borrado"),
            ) {
                Text("Cancelar")
            }
        },
    )
}
