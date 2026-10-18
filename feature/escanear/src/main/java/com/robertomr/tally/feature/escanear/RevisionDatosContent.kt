package com.robertomr.tally.feature.escanear

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.DatosRevision
import com.robertomr.tally.domain.expenses.entities.LineaEditable
import com.robertomr.tally.domain.expenses.entities.parsearCantidad
import com.robertomr.tally.domain.expenses.entities.parsearPrecio
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.TicketExtraido
import com.robertomr.tally.domain.expenses.entities.totalRecalculado
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Pantalla de revision y edicion del ticket digitalizado (spec revision-datos,
 * mockup 07; spec edicion-lineas, mockup 08; adaptados a DESIGN.md). Sobre la
 * extraccion tal cual salio del motor, el usuario corrige la cabecera
 * (comercio, fecha, total y categoria con chips de la enum fija de 6) y, en el
 * editor de lineas, anade, elimina y edita concepto/cantidad/precio.
 *
 * Contenedor de recibo normativo: superficie `surface-container-lowest`
 * (blanco), acento `primary` arriba, cabecera de comercio con icono
 * `storefront`, chips de categoria (activo `secondary-container` + check),
 * separador discontinuo antes del total y total destacado con `tnum`. Editor de
 * lineas (mockup 08): "Desglose de articulos" con contador REAL, filas
 * editables con borrar en paleta `error`, "Anadir producto o linea" con borde
 * discontinuo `primary`. Dock: "Total recalculado" (`tnum`, solo REFERENCIA) +
 * "Descartar" (outlined) / "Guardar cambios" (filled pill), >= 44dp.
 *
 * El campo "Total (con IVA)" se conserva (desviacion justificada del mockup 08):
 * el total es el dato autoritativo y NUNCA se sobrescribe con la suma de lineas
 * (docs/domain-model.md, "Total vs lineas").
 *
 * Decoracion ficticia PROHIBIDA (DESIGN.md Do's and Don'ts): NIF, direccion,
 * "Operacion #8294", "Deduccion IRPF", "Subtotal/Cuota IVA", IVA desglosado,
 * "HASH", "Registro contable", "Tarjeta Debito", "Almacenamiento cifrado",
 * "% verificado", contadores inventados, "Tally Vision". El badge de motor es
 * real.
 */
@Composable
fun RevisionDatosContent(
    extraido: TicketExtraido,
    datos: DatosRevision,
    errorGuardado: String?,
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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .testTag("revision_pantalla"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BadgeMotorRevision(
            motor = extraido.motor,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ContenedorRecibo(
                datos = datos,
                onComercioEditado = onComercioEditado,
                onFechaEditada = onFechaEditada,
                onTotalEditado = onTotalEditado,
                onCategoriaElegida = onCategoriaElegida,
            )
            EditorLineas(
                lineas = datos.lineas,
                onLineaAnadida = onLineaAnadida,
                onLineaConceptoEditado = onLineaConceptoEditado,
                onLineaCantidadEditada = onLineaCantidadEditada,
                onLineaPrecioEditado = onLineaPrecioEditado,
                onLineaEliminada = onLineaEliminada,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }

        if (errorGuardado != null) {
            Text(
                text = errorGuardado,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp)
                    .testTag("revision_error_guardado"),
            )
        }

        DockEdicion(
            lineas = datos.lineas,
            onDescartar = onDescartar,
            onGuardar = onGuardar,
        )
    }
}

/** Badge de motor (DESIGN.md): chip persistente con el motor real que extrajo. */
@Composable
private fun BadgeMotorRevision(
    motor: Motor,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceContainer,
                RoundedCornerShape(9999.dp),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(9999.dp),
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = when (motor) {
                Motor.LOCAL -> Icons.Outlined.Memory
                Motor.NUBE -> Icons.Outlined.Cloud
            },
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.secondary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = etiquetaMotor(motor),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag("revision_badge_motor"),
        )
    }
}

@Composable
private fun ContenedorRecibo(
    datos: DatosRevision,
    onComercioEditado: (String) -> Unit,
    onFechaEditada: (String) -> Unit,
    onTotalEditado: (String) -> Unit,
    onCategoriaElegida: (Categoria) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerLowest,
                RoundedCornerShape(16.dp),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp),
            ),
    ) {
        // Acento superior del recibo (mockup 07).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                ),
        )

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Cabecera de comercio (icono storefront) + fecha de emision.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainer,
                            RoundedCornerShape(12.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                CampoEditable(
                    etiqueta = "Comercio",
                    valorInicial = datos.comercio,
                    onValorCambiado = onComercioEditado,
                    testTag = "revision_comercio",
                    modifier = Modifier.weight(1f),
                )
            }

            CampoEditable(
                etiqueta = "Fecha de emisi\u00f3n",
                valorInicial = formatoFechaRevision(datos.fechaEmision),
                onValorCambiado = onFechaEditada,
                testTag = "revision_fecha",
            )

            ChipsCategoria(
                seleccionada = datos.categoria,
                onElegida = onCategoriaElegida,
            )

            SeparadorDiscontinuo(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            )

            CampoEditable(
                etiqueta = "Total (con IVA)",
                valorInicial = formatoTotalRevision(datos.total),
                onValorCambiado = onTotalEditado,
                testTag = "revision_total",
                keyboardType = KeyboardType.Decimal,
                cifrasTabulares = true,
                estilo = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            )
        }
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
    cifrasTabulares: Boolean = false,
    estilo: TextStyle = MaterialTheme.typography.bodyLarge,
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
        textStyle = if (cifrasTabulares) estilo.cifrasTabulares() else estilo,
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
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
 * concepto, cantidad y precio editables y boton borrar en paleta `error`.
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
            .background(
                MaterialTheme.colorScheme.surfaceContainerLowest,
                RoundedCornerShape(16.dp),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp),
            )
            .padding(16.dp)
            .testTag("revision_lineas"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Receipt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Desglose de art\u00edculos",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = contadorArticulos(lineas.size),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceContainer,
                        RoundedCornerShape(4.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .testTag("revision_lineas_contador"),
            )
        }

        lineas.forEach { linea ->
            key(linea.id) {
                FilaLinea(
                    linea = linea,
                    onConceptoEditado = { onLineaConceptoEditado(linea.id, it) },
                    onCantidadEditada = { onLineaCantidadEditada(linea.id, it) },
                    onPrecioEditado = { onLineaPrecioEditado(linea.id, it) },
                    onEliminar = { onLineaEliminada(linea.id) },
                )
            }
        }

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
            .background(
                MaterialTheme.colorScheme.surfaceContainerLow,
                RoundedCornerShape(8.dp),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(8.dp)
            .testTag(prefijo),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CampoEditable(
            etiqueta = "Concepto",
            valorInicial = linea.concepto,
            onValorCambiado = onConceptoEditado,
            testTag = "${prefijo}_concepto",
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            CampoEditable(
                etiqueta = "Cant.",
                valorInicial = linea.cantidad.toString(),
                onValorCambiado = onCantidadEditada,
                testTag = "${prefijo}_cantidad",
                keyboardType = KeyboardType.Number,
                cifrasTabulares = true,
                esValido = { parsearCantidad(it) != null },
                modifier = Modifier.width(96.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            CampoEditable(
                etiqueta = "Precio (\u20ac)",
                valorInicial = formatoPrecioLinea(linea.precioUnitario),
                onValorCambiado = onPrecioEditado,
                testTag = "${prefijo}_precio",
                keyboardType = KeyboardType.Decimal,
                cifrasTabulares = true,
                esValido = { parsearPrecio(it) != null },
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(4.dp))
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
                    contentDescription = "Eliminar ${linea.concepto.ifBlank { "l\u00ednea" }}",
                )
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
            text = "A\u00f1adir producto o l\u00ednea",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * Dock inferior en edicion (mockup 08): "Total recalculado" como REFERENCIA
 * (suma de lineas con [totalRecalculado]; no toca el total del ticket) y
 * acciones "Descartar" (outlined, no escribe) / "Guardar cambios" (filled).
 */
@Composable
private fun DockEdicion(
    lineas: List<LineaEditable>,
    onDescartar: () -> Unit,
    onGuardar: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
                Text(
                    text = "Calculado a partir de ${contadorArticulos(lineas.size)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
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
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onDescartar,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
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
            Button(
                onClick = onGuardar,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
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
                Text(text = "Guardar cambios", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Contador REAL de filas (DESIGN.md: nada de "6 articulos" inventados). */
private fun contadorArticulos(n: Int): String =
    if (n == 1) "1 art\u00edculo" else "$n art\u00edculos"

/** Precio de linea editable: vacio si es 0 (fila nueva lista para escribir). */
private fun formatoPrecioLinea(precio: Double): String =
    if (precio == 0.0) "" else formatoTotalRevision(precio)

/** Separador discontinuo antes del total (mockup 07). */
@Composable
private fun SeparadorDiscontinuo(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier = modifier.height(1.dp)) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
        )
    }
}

private fun iconoCategoria(categoria: Categoria): ImageVector = when (categoria) {
    Categoria.ALIMENTACION -> Icons.Outlined.Restaurant
    Categoria.HOGAR -> Icons.Outlined.Home
    Categoria.TRANSPORTE -> Icons.Outlined.DirectionsCar
    Categoria.OCIO -> Icons.Outlined.SportsEsports
    Categoria.SALUD -> Icons.Outlined.LocalHospital
    Categoria.OTROS -> Icons.Outlined.Category
}

private val FORMATO_FECHA_REVISION: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d/M/yyyy")

private fun formatoFechaRevision(fecha: LocalDateTime): String =
    fecha.format(FORMATO_FECHA_REVISION)

private fun formatoTotalRevision(total: Double): String =
    String.format(Locale.forLanguageTag("es-ES"), "%.2f", total)

/** Cifras tabulares (DESIGN.md: "tnum" en cualquier cifra). */
private fun TextStyle.cifrasTabulares(): TextStyle = copy(fontFeatureSettings = "tnum")
