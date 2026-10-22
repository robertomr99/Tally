package com.robertomr.tally.feature.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.AvisoPago
import com.robertomr.tally.domain.expenses.entities.DatosRevision
import com.robertomr.tally.domain.expenses.entities.FormaPago
import com.robertomr.tally.domain.expenses.entities.LineaEditable
import com.robertomr.tally.domain.expenses.entities.avisoPago
import com.robertomr.tally.domain.expenses.entities.parsearCantidad
import com.robertomr.tally.domain.expenses.entities.parsearPrecio
import com.robertomr.tally.domain.expenses.entities.totalRecalculado
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/*
 * Formulario editable de un ticket (spec revision-datos, mockup 07; spec
 * edicion-lineas, mockup 08; movido aqui por la spec detalle-ticket, D1) que
 * comparten la revision del ticket recien digitalizado (`:feature:escanear`) y
 * el detalle del ticket guardado (`:feature:gastos`) sin que ambos features se
 * conozcan. Los testTags `revision_*` se conservan literalmente: los usan los
 * tests instrumentados de ambos flujos.
 *
 * Decoracion ficticia PROHIBIDA (DESIGN.md Do's and Don'ts): NIF, direccion,
 * IVA desglosado, hash, "% verificado", etc.
 */

/**
 * Contenedor de recibo (cabecera editable) + editor de lineas. Va dentro de la
 * columna con scroll que aporta la pantalla.
 *
 * El campo "Total (con IVA)" se conserva (desviacion justificada del mockup 08):
 * el total es el dato autoritativo y NUNCA se sobrescribe con la suma de lineas
 * (docs/domain-model.md, "Total vs lineas").
 *
 * Cada campo guarda su texto con `rememberSaveable(valorInicial)`: para
 * restablecer el formulario (p. ej. "Descartar") la pantalla debe re-crearlo con
 * `key(version)`.
 */
@Composable
fun FormularioTicketEditable(
    datos: DatosRevision,
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
    modifier: Modifier = Modifier,
) {
    // Un solo papel (spec papel-ticket, D5), en orden de recibo: cabecera,
    // punteado, desglose de articulos, punteado y total al final.
    PapelTicket(modifier = modifier.padding(bottom = 16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CampoEditable(
                etiqueta = "Comercio",
                valorInicial = datos.comercio,
                onValorCambiado = onComercioEditado,
                testTag = "revision_comercio",
                estilo = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                alineacion = TextAlign.Center,
            )
            CampoEditable(
                etiqueta = "Fecha de emisión",
                valorInicial = formatoFechaRevision(datos.fechaEmision),
                onValorCambiado = onFechaEditada,
                testTag = "revision_fecha",
                alineacion = TextAlign.Center,
            )
            ChipsCategoria(
                seleccionada = datos.categoria,
                onElegida = onCategoriaElegida,
            )
        }

        SeparadorPunteado(modifier = Modifier.padding(vertical = 16.dp))

        EditorLineas(
            lineas = datos.lineas,
            onLineaAnadida = onLineaAnadida,
            onLineaConceptoEditado = onLineaConceptoEditado,
            onLineaCantidadEditada = onLineaCantidadEditada,
            onLineaPrecioEditado = onLineaPrecioEditado,
            onLineaEliminada = onLineaEliminada,
        )

        SeparadorPunteado(modifier = Modifier.padding(vertical = 16.dp))

        // Total autoritativo al final del recibo, destacado y a la derecha.
        CampoEditable(
            etiqueta = "Total (con IVA)",
            valorInicial = formatoTotalRevision(datos.total),
            onValorCambiado = onTotalEditado,
            testTag = "revision_total",
            keyboardType = KeyboardType.Decimal,
            estilo = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            alineacion = TextAlign.End,
        )

        // Forma de pago bajo el total, como en un recibo (spec forma-pago).
        SeparadorPunteado(modifier = Modifier.padding(vertical = 16.dp))
        SeccionPago(
            datos = datos,
            onFormaPagoElegida = onFormaPagoElegida,
            onEntregadoEditado = onEntregadoEditado,
            onCambioEditado = onCambioEditado,
        )
    }
}

@Composable
private fun CampoEditable(
    etiqueta: String,
    valorInicial: String,
    onValorCambiado: (String) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    estilo: TextStyle = MaterialTheme.typography.bodyLarge,
    alineacion: TextAlign = TextAlign.Start,
    esValido: (String) -> Boolean = { true },
) {
    // El texto local es la fuente durante la edicion: el ViewModel parsea y
    // actualiza el dato, pero el campo no se re-formatea bajo los dedos.
    var texto by rememberSaveable { mutableStateOf(valorInicial) }
    OutlinedTextField(
        value = texto,
        onValueChange = {
            texto = it
            onValorCambiado(it)
        },
        label = { Text(etiqueta) },
        singleLine = true,
        // Un texto no valido se marca en error: el modelo conserva el ultimo
        // valor valido, y asi el usuario ve que lo escrito no es lo que cuenta.
        isError = !esValido(texto),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        // Tipografia de recibo solo en el texto escrito; la etiqueta sigue en Inter.
        textStyle = reciboTexto(estilo).copy(textAlign = alineacion),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
    )
}

/**
 * Chips de categoria (radio-group, mockup 07): SOLO las 6 de [Categoria.entries];
 * activo en `secondary-container` + `on-secondary-container` con icono check
 * (sin depender solo del color, DESIGN.md).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipsCategoria(
    seleccionada: Categoria,
    onElegida: (Categoria) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag("revision_categoria"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Categoria.entries.forEach { categoria ->
            val activa = categoria == seleccionada
            val contenedor = if (activa) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
            val contenido = if (activa) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            Row(
                modifier = Modifier
                    // Área táctil de 48 dp sin agrandar el chip visual (V16).
                    .minimumInteractiveComponentSize()
                    .selectable(
                        selected = activa,
                        role = Role.RadioButton,
                        onClick = { onElegida(categoria) },
                    )
                    .background(contenedor, RoundedCornerShape(9999.dp))
                    .border(
                        width = 1.dp,
                        color = if (activa) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        },
                        shape = RoundedCornerShape(9999.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("revision_categoria_${categoria.name}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = iconoCategoria(categoria),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = contenido,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = etiquetaCategoria(categoria),
                    style = MaterialTheme.typography.labelMedium,
                    color = contenido,
                )
                if (activa) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = contenido,
                    )
                }
            }
        }
    }
}

/**
 * Editor de lineas (mockup 08, "Desglose de articulos"): filas keyeadas por
 * [LineaEditable.id] (R-EDI2: borrar/anadir no "migra" el texto tecleado), con
 * concepto, cantidad y precio editables y boton borrar en paleta `error`. Va
 * dentro del papel: las filas se separan con punteado, sin tarjetas anidadas.
 */
@Composable
private fun EditorLineas(
    lineas: List<LineaEditable>,
    onLineaAnadida: () -> Unit,
    onLineaConceptoEditado: (Long, String) -> Unit,
    onLineaCantidadEditada: (Long, String) -> Unit,
    onLineaPrecioEditado: (Long, String) -> Unit,
    onLineaEliminada: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("revision_lineas"),
    ) {
        Row(
            modifier = Modifier.padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TituloSeccion("Desglose de artículos", modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = contadorArticulos(lineas.size),
                style = reciboTexto(MaterialTheme.typography.labelSmall)
                    .copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("revision_lineas_contador"),
            )
        }

        lineas.forEachIndexed { indice, linea ->
            key(linea.id) {
                if (indice > 0) SeparadorPunteado(modifier = Modifier.padding(vertical = 10.dp))
                FilaLinea(
                    linea = linea,
                    onConceptoEditado = { onLineaConceptoEditado(linea.id, it) },
                    onCantidadEditada = { onLineaCantidadEditada(linea.id, it) },
                    onPrecioEditado = { onLineaPrecioEditado(linea.id, it) },
                    onEliminar = { onLineaEliminada(linea.id) },
                )
            }
        }

        if (lineas.isNotEmpty()) SeparadorPunteado(modifier = Modifier.padding(vertical = 10.dp))
        BotonAnadirLinea(onClick = onLineaAnadida)
    }
}

@Composable
private fun FilaLinea(
    linea: LineaEditable,
    onConceptoEditado: (String) -> Unit,
    onCantidadEditada: (String) -> Unit,
    onPrecioEditado: (String) -> Unit,
    onEliminar: () -> Unit,
) {
    val prefijo = "revision_linea_${linea.id}"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(prefijo),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CampoEditable(
            etiqueta = "Concepto",
            valorInicial = linea.concepto,
            onValorCambiado = onConceptoEditado,
            testTag = "${prefijo}_concepto",
        )
        // Con fuentes muy grandes (>1,4) cantidad y precio no caben en una fila
        // sin que las etiquetas envuelvan: se apilan (cantidad / precio + borrar).
        val apilar = LocalDensity.current.fontScale > 1.4f
        val campoCantidad: @Composable (Modifier) -> Unit = { m ->
            CampoEditable(
                etiqueta = "Cant.",
                valorInicial = linea.cantidad.toString(),
                onValorCambiado = onCantidadEditada,
                testTag = "${prefijo}_cantidad",
                keyboardType = KeyboardType.Number,
                esValido = { parsearCantidad(it) != null },
                modifier = m,
            )
        }
        val campoPrecio: @Composable (Modifier) -> Unit = { m ->
            CampoEditable(
                etiqueta = "Precio (€)",
                valorInicial = formatoPrecioLinea(linea.precioUnitario),
                onValorCambiado = onPrecioEditado,
                testTag = "${prefijo}_precio",
                keyboardType = KeyboardType.Decimal,
                esValido = { parsearPrecio(it) != null },
                alineacion = TextAlign.End,
                modifier = m,
            )
        }
        val botonBorrar: @Composable () -> Unit = {
            IconButton(
                onClick = onEliminar,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("${prefijo}_borrar"),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Eliminar ${linea.concepto.ifBlank { "línea" }}",
                )
            }
        }
        if (apilar) {
            campoCantidad(Modifier)
            Row(verticalAlignment = Alignment.CenterVertically) {
                campoPrecio(Modifier.weight(1f))
                Spacer(modifier = Modifier.width(4.dp))
                botonBorrar()
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                campoCantidad(Modifier.width(96.dp))
                Spacer(modifier = Modifier.width(8.dp))
                campoPrecio(Modifier.weight(1f))
                Spacer(modifier = Modifier.width(4.dp))
                botonBorrar()
            }
        }
    }
}

/** "Anadir producto o linea" (mockup 08): borde discontinuo `primary`, >= 44dp. */
@Composable
private fun BotonAnadirLinea(onClick: () -> Unit) {
    val colorBorde = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .drawBehind {
                drawRoundRect(
                    color = colorBorde,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)),
                    ),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                )
            }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("revision_anadir_linea"),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.AddCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Añadir producto o línea",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * Dock inferior en edicion (mockup 08): "Total recalculado" como REFERENCIA
 * (suma de lineas con [totalRecalculado]; no toca el total del ticket) y
 * acciones "Descartar" (outlined, no escribe) / "Guardar cambios" (filled).
 *
 * @param guardarHabilitado `false` deshabilita "Guardar cambios" (p. ej. sin
 * cambios o guardando en el detalle).
 * @param descartarHabilitado `false` deshabilita "Descartar".
 * @param textoGuardar etiqueta del botón principal ("Guardar gasto" en Nuevo gasto).
 * @param mostrarTotalRecalculado `false` oculta el bloque "Total recalculado" (Nuevo gasto
 * sin líneas: la suma de líneas no es el total que el usuario escribió).
 */
@Composable
fun DockEdicion(
    lineas: List<LineaEditable>,
    onDescartar: () -> Unit,
    onGuardar: () -> Unit,
    modifier: Modifier = Modifier,
    guardarHabilitado: Boolean = true,
    descartarHabilitado: Boolean = true,
    textoGuardar: String = "Guardar cambios",
    mostrarTotalRecalculado: Boolean = true,
) {
    // El fondo llega a todo el ancho; el contenido va en la columna centrada.
    ColumnaCentrada(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        ContenidoDock(
            lineas, onDescartar, onGuardar, guardarHabilitado, descartarHabilitado,
            textoGuardar, mostrarTotalRecalculado,
        )
    }
}

@Composable
private fun ContenidoDock(
    lineas: List<LineaEditable>,
    onDescartar: () -> Unit,
    onGuardar: () -> Unit,
    guardarHabilitado: Boolean,
    descartarHabilitado: Boolean,
    textoGuardar: String,
    mostrarTotalRecalculado: Boolean,
) {
    // Con fuente grande el dock no cabe en una fila (V03): los botones se apilan
    // (Guardar primero) y el subtítulo del total baja a ancho completo.
    val apilar = LocalDensity.current.fontScale > UMBRAL_APILAR_BOTONES
    val botonDescartar: @Composable (Modifier) -> Unit = { modificador ->
        OutlinedButton(
            onClick = onDescartar,
            enabled = descartarHabilitado,
            modifier = modificador
                .heightIn(min = 48.dp)
                .testTag("revision_descartar"),
            shape = RoundedCornerShape(9999.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Descartar", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    val botonGuardar: @Composable (Modifier) -> Unit = { modificador ->
        Button(
            onClick = onGuardar,
            enabled = guardarHabilitado,
            modifier = modificador
                .heightIn(min = 48.dp)
                .testTag("revision_guardar"),
            shape = RoundedCornerShape(9999.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            colors = ButtonDefaults.buttonColors(),
        ) {
            Icon(
                imageVector = Icons.Outlined.Save,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = textoGuardar, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = PADDING_PANTALLA, end = PADDING_PANTALLA, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (mostrarTotalRecalculado) Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Functions,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Total recalculado",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!apilar) SubtituloTotalRecalculado(lineas)
            }
            Text(
                text = formatoImporte(totalRecalculado(lineas)),
                style = MaterialTheme.typography.headlineSmall
                    .copy(fontWeight = FontWeight.SemiBold)
                    .cifrasTabulares(),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("revision_total_recalculado"),
            )
        }
        if (apilar) {
            if (mostrarTotalRecalculado) SubtituloTotalRecalculado(lineas)
            botonGuardar(Modifier.fillMaxWidth())
            botonDescartar(Modifier.fillMaxWidth())
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // "Guardar cambios" es la etiqueta larga: más peso para que no se trunque en 360 dp.
                botonDescartar(Modifier.weight(1f))
                botonGuardar(Modifier.weight(1.3f))
            }
        }
    }
}

@Composable
private fun SubtituloTotalRecalculado(lineas: List<LineaEditable>) {
    Text(
        text = "Calculado a partir de ${contadorArticulos(lineas.size)}",
        style = MaterialTheme.typography.labelSmall,
        // onSurfaceVariant (no outline): el texto de 11 sp necesita contraste AA (V23).
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Contador REAL de filas (DESIGN.md: nada de "6 articulos" inventados). */
private fun contadorArticulos(n: Int): String =
    if (n == 1) "1 artículo" else "$n artículos"

/** Precio de linea editable: vacio si es 0 (fila nueva lista para escribir). */
private fun formatoPrecioLinea(precio: Double): String =
    if (precio == 0.0) "" else formatoTotalRevision(precio)

private val FORMATO_FECHA_REVISION: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d/M/yyyy")

private fun formatoFechaRevision(fecha: LocalDateTime): String =
    fecha.format(FORMATO_FECHA_REVISION)

private fun formatoTotalRevision(total: Double): String =
    String.format(Locale.forLanguageTag("es-ES"), "%.2f", total)

/** Cifras tabulares (DESIGN.md: "tnum" en cualquier cifra). */
private fun TextStyle.cifrasTabulares(): TextStyle = copy(fontFeatureSettings = "tnum")

// ---------------------------------------------------------------------------
// Forma de pago (spec forma-pago)
// ---------------------------------------------------------------------------

/**
 * Seccion de pago bajo el total, como en un recibo: selector Efectivo / Tarjeta /
 * No indicada y, solo con efectivo, "Entregado" y "Cambio" (informativos: el
 * total sigue siendo lo que cuesta) con un aviso NO bloqueante si no cuadran.
 * Al pasar a Tarjeta los campos se ocultan pero sus valores se conservan hasta
 * guardar (se limpian al guardar, `DatosRevision.normalizada()`).
 */
@Composable
private fun SeccionPago(
    datos: DatosRevision,
    onFormaPagoElegida: (FormaPago) -> Unit,
    onEntregadoEditado: (String) -> Unit,
    onCambioEditado: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("revision_pago"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TituloSeccion("Forma de pago")
        SelectorFormaPago(seleccionada = datos.formaPago, onElegida = onFormaPagoElegida)
        if (datos.formaPago == FormaPago.EFECTIVO) {
            CampoImporteOpcional(
                etiqueta = "Entregado (€)",
                valor = datos.entregado,
                onValorCambiado = onEntregadoEditado,
                testTag = "revision_entregado",
            )
            CampoImporteOpcional(
                etiqueta = "Cambio (€)",
                valor = datos.cambio,
                onValorCambiado = onCambioEditado,
                testTag = "revision_cambio",
            )
            AvisoDePago(avisoPago(datos))
        }
    }
}

/**
 * Selector segmentado accesible (radio-group): cada opcion mide >= 48 dp, lleva
 * icono + texto + marca de seleccionada (no solo color) y expone `selected` y
 * `Role.RadioButton`. Con fuente grande (> [UMBRAL_APILAR_BOTONES]) las opciones
 * se apilan a ancho completo.
 */
@Composable
private fun SelectorFormaPago(
    seleccionada: FormaPago,
    onElegida: (FormaPago) -> Unit,
    modifier: Modifier = Modifier,
) {
    val apilar = LocalDensity.current.fontScale > UMBRAL_APILAR_BOTONES
    val opcion: @Composable (FormaPago, Modifier) -> Unit = { forma, m ->
        OpcionFormaPago(
            forma = forma,
            activa = forma == seleccionada,
            enColumna = !apilar,
            onClick = { onElegida(forma) },
            modifier = m,
        )
    }
    if (apilar) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .selectableGroup()
                .testTag("revision_forma_pago"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FormaPago.entries.forEach { opcion(it, Modifier.fillMaxWidth()) }
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .selectableGroup()
                .testTag("revision_forma_pago"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FormaPago.entries.forEach { opcion(it, Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun OpcionFormaPago(
    forma: FormaPago,
    activa: Boolean,
    enColumna: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contenedor = if (activa) {
        contenedorFormaPago(forma)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val contenido = if (activa) {
        contenidoFormaPago(forma)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val forma12 = RoundedCornerShape(12.dp)
    val icono: @Composable () -> Unit = {
        Icon(
            imageVector = iconoFormaPago(forma),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = contenido,
        )
    }
    val marca: @Composable () -> Unit = {
        if (activa) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = contenido,
            )
        }
    }
    val etiqueta: @Composable (Modifier) -> Unit = { m ->
        Text(
            text = etiquetaFormaPago(forma),
            style = MaterialTheme.typography.labelMedium,
            color = contenido,
            textAlign = TextAlign.Center,
            modifier = m,
        )
    }
    val marco = modifier
        .heightIn(min = 48.dp)
        .clip(forma12)
        .background(contenedor)
        .border(
            width = if (activa) 2.dp else 1.dp,
            color = if (activa) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            },
            shape = forma12,
        )
        .selectable(selected = activa, role = Role.RadioButton, onClick = onClick)
        .padding(horizontal = 8.dp, vertical = 8.dp)
        .testTag("revision_forma_pago_${forma.name.lowercase()}")
    if (enColumna) {
        // Tres opciones en fila (360 dp): icono (+ marca) encima y texto debajo, para
        // que ninguna etiqueta se parta a mitad de palabra.
        Column(
            modifier = marco,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icono()
                if (activa) Spacer(modifier = Modifier.width(2.dp))
                marca()
            }
            etiqueta(Modifier)
        }
    } else {
        // Opciones apiladas a ancho completo (fuente grande): icono + texto + marca en una fila.
        Row(
            modifier = marco,
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icono()
            Spacer(modifier = Modifier.width(6.dp))
            etiqueta(Modifier.weight(1f, fill = false))
            if (activa) Spacer(modifier = Modifier.width(4.dp))
            marca()
        }
    }
}

/**
 * Campo de importe opcional (entregado / cambio): vacio = sin dato. El texto
 * local manda mientras se escribe, pero si el dato cambia por otra via (el cambio
 * se recalcula al editar entregado o total) el campo se actualiza.
 */
@Composable
private fun CampoImporteOpcional(
    etiqueta: String,
    valor: Double?,
    onValorCambiado: (String) -> Unit,
    testTag: String,
) {
    var texto by rememberSaveable { mutableStateOf(formatoImporteOpcional(valor)) }
    LaunchedEffect(valor) {
        if (!coincideImporte(texto, valor)) texto = formatoImporteOpcional(valor)
    }
    OutlinedTextField(
        value = texto,
        onValueChange = {
            texto = it
            onValorCambiado(it)
        },
        label = { Text(etiqueta) },
        singleLine = true,
        isError = texto.isNotBlank() && parsearImporteOpcional(texto) == null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = reciboTexto(MaterialTheme.typography.bodyLarge).copy(textAlign = TextAlign.End),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
    )
}

/** Aviso no bloqueante (texto + icono, no solo color) de coherencia entregado/cambio. */
@Composable
private fun AvisoDePago(aviso: AvisoPago?) {
    if (aviso == null) return
    val texto = when (aviso) {
        AvisoPago.ENTREGADO_MENOR_QUE_TOTAL ->
            "Aviso: lo entregado es menor que el total. Puedes guardar igualmente."
        AvisoPago.CAMBIO_NO_CUADRA ->
            "Aviso: el cambio no coincide con entregado menos total. Puedes guardar igualmente."
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .testTag("revision_aviso_pago"),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Outlined.WarningAmber,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = texto,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun parsearImporteOpcional(texto: String): Double? =
    texto.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }

private fun formatoImporteOpcional(valor: Double?): String =
    valor?.let(::formatoTotalRevision).orEmpty()

/** `true` si el texto escrito ya representa [valor] (vacio = sin dato). */
private fun coincideImporte(texto: String, valor: Double?): Boolean {
    if (texto.isBlank()) return valor == null
    val actual = parsearImporteOpcional(texto)
    return actual != null && valor != null && abs(actual - valor) < 0.005
}
