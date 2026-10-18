package com.robertomr.tally.feature.ajustes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.RadioButton
import androidx.compose.ui.semantics.Role
import com.robertomr.tally.domain.expenses.entities.PreferenciaMotor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.robertomr.tally.domain.expenses.entities.EstadoModelo
import com.robertomr.tally.domain.expenses.entities.ModeloLocal
import com.robertomr.tally.domain.expenses.entities.MotivoFallo
import com.robertomr.tally.domain.expenses.entities.MotivoPausa
import java.util.Locale

/**
 * Pantalla Ajustes: seccion "Motor de extraccion" (Auto/Local/Nube, spec
 * ajustes-motor) y seccion "Modelo local" (estado, progreso,
 * descargar/cancelar/eliminar y token de Hugging Face, spec descarga-modelo). La pantalla recibe callbacks, nunca el
 * NavController.
 */
@Composable
fun AjustesScreen(onBack: () -> Unit) {
    val viewModel: AjustesViewModel = hiltViewModel()
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val estadoVisual = rememberAjustesState()

    AjustesContent(
        ui = ui,
        estadoVisual = estadoVisual,
        onBack = onBack,
        onElegirMotor = viewModel::onElegirMotor,
        onDescargar = viewModel::onDescargar,
        onCancelar = viewModel::onCancelar,
        onEliminar = viewModel::onEliminar,
        onGuardarToken = viewModel::onGuardarToken,
        onBorrarToken = viewModel::onBorrarToken,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AjustesContent(
    ui: AjustesUiState,
    estadoVisual: AjustesState,
    onBack: () -> Unit,
    onElegirMotor: (PreferenciaMotor) -> Unit,
    onDescargar: () -> Unit,
    onCancelar: () -> Unit,
    onEliminar: () -> Unit,
    onGuardarToken: (String) -> Unit,
    onBorrarToken: () -> Unit,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("pantalla_ajustes"),
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("ajustes_volver")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SeccionMotor(ui = ui, onElegir = onElegirMotor)
                SeccionModeloLocal(
                    ui = ui,
                    onDescargar = onDescargar,
                    onCancelar = onCancelar,
                    onPedirEliminar = estadoVisual::pedirConfirmarEliminar,
                )
                SeccionToken(
                    hayToken = ui.hayToken,
                    escrito = estadoVisual.tokenEscrito,
                    onEscribir = estadoVisual::onTokenEscrito,
                    onGuardar = {
                        onGuardarToken(estadoVisual.tokenEscrito)
                        estadoVisual.limpiarToken()
                    },
                    onBorrar = onBorrarToken,
                )
            }
        }
    }

    if (estadoVisual.confirmandoEliminar) {
        AlertDialog(
            onDismissRequest = estadoVisual::cerrarConfirmarEliminar,
            title = { Text("Eliminar modelo") },
            text = { Text("Se borrará el modelo descargado (3,4 GB). Podrás volver a descargarlo.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        estadoVisual.cerrarConfirmarEliminar()
                        onEliminar()
                    },
                    modifier = Modifier.testTag("confirmar_eliminar_modelo"),
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = estadoVisual::cerrarConfirmarEliminar) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun SeccionMotor(
    ui: AjustesUiState,
    onElegir: (PreferenciaMotor) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("seccion_motor"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Motor de extracción", style = MaterialTheme.typography.titleMedium)
            Text(
                "Se aplica a la siguiente extracción. Auto usa Local si el dispositivo puede " +
                    "y el modelo está descargado; si no, Nube.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(Modifier.selectableGroup()) {
                OpcionMotor("Auto", PreferenciaMotor.AUTO, "motor_opcion_auto", ui.preferencia, onElegir)
                OpcionMotor("Local", PreferenciaMotor.LOCAL, "motor_opcion_local", ui.preferencia, onElegir)
                OpcionMotor("Nube", PreferenciaMotor.NUBE, "motor_opcion_nube", ui.preferencia, onElegir)
            }
            if (ui.avisoLocal) {
                Text(
                    text = "Local necesita el modelo descargado; sin él la extracción fallará",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("motor_aviso_local"),
                )
            }
        }
    }
}

@Composable
private fun OpcionMotor(
    etiqueta: String,
    valor: PreferenciaMotor,
    tag: String,
    seleccionada: PreferenciaMotor,
    onElegir: (PreferenciaMotor) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = seleccionada == valor,
                onClick = { onElegir(valor) },
                role = Role.RadioButton,
            )
            .padding(vertical = 4.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = seleccionada == valor, onClick = null)
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun SeccionModeloLocal(
    ui: AjustesUiState,
    onDescargar: () -> Unit,
    onCancelar: () -> Unit,
    onPedirEliminar: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("seccion_modelo_local"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Modelo local", style = MaterialTheme.typography.titleMedium)
            Text(
                "Gemma 3n E2B · extracción sin red, el ticket no sale del dispositivo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (ui.cargando) {
                Text("Comprobando…", style = MaterialTheme.typography.bodyMedium)
                return@Column
            }

            val estado = ui.estado
            Text(
                text = etiquetaEstado(estado),
                style = MaterialTheme.typography.titleSmall,
                color = if (estado is EstadoModelo.Fallo) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.testTag("modelo_estado"),
            )

            when (estado) {
                is EstadoModelo.Descargando -> Progreso(estado.bytes, estado.total)
                is EstadoModelo.EnPausa -> Progreso(estado.bytes, estado.total)
                else -> Unit
            }
            detalleEstado(estado)?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium.cifrasTabulares(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("modelo_detalle"),
                )
            }

            ui.aviso?.let { aviso ->
                Text(
                    text = textoAviso(aviso),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("modelo_aviso"),
                )
            }

            when (estado) {
                EstadoModelo.NoDescargado, is EstadoModelo.Fallo ->
                    Button(
                        onClick = onDescargar,
                        modifier = Modifier.testTag("boton_descargar_modelo"),
                    ) { Text(if (estado is EstadoModelo.Fallo) "Reintentar descarga" else "Descargar modelo") }
                is EstadoModelo.Descargando, is EstadoModelo.EnPausa ->
                    OutlinedButton(
                        onClick = onCancelar,
                        modifier = Modifier.testTag("boton_cancelar_descarga"),
                    ) { Text("Cancelar") }
                is EstadoModelo.Descargado ->
                    OutlinedButton(
                        onClick = onPedirEliminar,
                        modifier = Modifier.testTag("boton_eliminar_modelo"),
                    ) { Text("Eliminar modelo") }
            }
        }
    }
}

@Composable
private fun Progreso(bytes: Long, total: Long) {
    val fraccion = if (total > 0) (bytes.toFloat() / total).coerceIn(0f, 1f) else 0f
    LinearProgressIndicator(
        progress = { fraccion },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("modelo_progreso"),
        color = MaterialTheme.colorScheme.secondary,
        trackColor = MaterialTheme.colorScheme.secondaryContainer,
    )
}

@Composable
private fun SeccionToken(
    hayToken: Boolean,
    escrito: String,
    onEscribir: (String) -> Unit,
    onGuardar: () -> Unit,
    onBorrar: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Token de Hugging Face", style = MaterialTheme.typography.titleMedium)
            Text(
                "El modelo exige aceptar su licencia en Hugging Face. Pega aquí un token de " +
                    "solo lectura; se guarda solo en este dispositivo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (hayToken) "Token guardado" else "Sin token",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.testTag("token_estado"),
            )
            OutlinedTextField(
                value = escrito,
                onValueChange = onEscribir,
                label = { Text("Token") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("campo_token"),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onGuardar,
                    enabled = escrito.isNotBlank(),
                    modifier = Modifier.testTag("boton_guardar_token"),
                ) { Text("Guardar token") }
                if (hayToken) {
                    OutlinedButton(
                        onClick = onBorrar,
                        modifier = Modifier.testTag("boton_borrar_token"),
                    ) { Text("Borrar token") }
                }
            }
        }
    }
}

private fun etiquetaEstado(estado: EstadoModelo): String = when (estado) {
    EstadoModelo.NoDescargado -> "No descargado"
    is EstadoModelo.Descargando -> "Descargando"
    is EstadoModelo.EnPausa -> when (estado.motivo) {
        MotivoPausa.EsperandoWifi -> "En pausa · Esperando WiFi"
    }
    is EstadoModelo.Descargado -> "Descargado"
    is EstadoModelo.Fallo -> "Error"
}

private fun detalleEstado(estado: EstadoModelo): String? = when (estado) {
    EstadoModelo.NoDescargado ->
        "Se descargan ${formatearTamano(ModeloLocal.TAMANO_ESPERADO_BYTES)} solo con WiFi."
    is EstadoModelo.Descargando -> formatearProgreso(estado.bytes, estado.total)
    is EstadoModelo.EnPausa -> formatearProgreso(estado.bytes, estado.total)
    is EstadoModelo.Descargado -> "${formatearTamano(estado.bytes)} en el dispositivo"
    is EstadoModelo.Fallo -> when (estado.motivo) {
        MotivoFallo.Autorizacion -> "Acepta la licencia en Hugging Face y revisa tu token"
        MotivoFallo.Integridad ->
            "El tamaño del fichero no coincide con el anunciado; se descartó la descarga"
        MotivoFallo.Red -> "No se pudo completar la descarga. Inténtalo de nuevo"
        MotivoFallo.SinEspacio -> "Sin espacio suficiente en el dispositivo"
    }
}

private fun textoAviso(aviso: AvisoDescarga): String = when (aviso) {
    AvisoDescarga.SoloWifi -> "Solo se descarga con WiFi"
    is AvisoDescarga.EspacioInsuficiente ->
        "Necesitas ${formatearTamano(aviso.necesarios)} libres (tienes ${formatearTamano(aviso.libres)})"
}

private const val GB = 1024.0 * 1024.0 * 1024.0
private const val MB = 1024.0 * 1024.0
private val LOCALE_ES = Locale.forLanguageTag("es-ES")

/** GB binarios (1024^3) con un decimal y coma, p. ej. 3.655.827.456 -> "3,4". */
internal fun formatearGb(bytes: Long): String = String.format(LOCALE_ES, "%.1f", bytes / GB)

/** "3,4 GB"; por debajo de 1 GB se usa MB entero ("512 MB") para no mostrar "0,0 GB". */
internal fun formatearTamano(bytes: Long): String =
    if (bytes >= GB) "${formatearGb(bytes)} GB" else String.format(LOCALE_ES, "%d MB", (bytes / MB).toLong())

/** "1,2 / 3,4 GB" o "512 MB / 3,4 GB". */
internal fun formatearProgreso(bytes: Long, total: Long): String =
    if (bytes >= GB) "${formatearGb(bytes)} / ${formatearTamano(total)}"
    else "${formatearTamano(bytes)} / ${formatearTamano(total)}"

/** Cifras tabulares (DESIGN.md: "tnum" en cualquier cifra). */
private fun TextStyle.cifrasTabulares(): TextStyle = copy(fontFeatureSettings = "tnum")
