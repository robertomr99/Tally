package com.robertomr.tally.feature.escanear

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.DatosRevision
import com.robertomr.tally.domain.expenses.entities.Linea
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.TicketExtraido
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Pantalla 07: revision del ticket digitalizado (spec revision-datos, mockup 07
 * adaptado a DESIGN.md). Sobre la extraccion tal cual salio del motor, el
 * usuario corrige SOLO la cabecera (comercio, fecha, total y categoria con
 * chips de la enum fija de 6); las lineas se muestran en solo lectura
 * (`edicion-lineas` es otra feature).
 *
 * Contenedor de recibo normativo del mockup: superficie
 * `surface-container-lowest` (blanco), acento `primary` arriba, cabecera de
 * comercio con icono `storefront`, chips de categoria (activo
 * `secondary-container` + check), filas de linea (concepto x cantidad +
 * importe con `tnum`), separador discontinuo antes del total y total destacado
 * con `tnum`; dock inferior con "Guardar" (filled pill, >= 44dp).
 *
 * Decoracion ficticia PROHIBIDA (DESIGN.md Do's and Don'ts): NIF, direccion,
 * "Operacion #8294", "Deduccion IRPF", "Subtotal/Cuota IVA", "HASH", "Registro
 * contable", "Tarjeta Debito", "Almacenamiento cifrado", "% verificado",
 * "6 items detectados", "Tally Vision". El badge de motor es real.
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
        ) {
            ContenedorRecibo(
                datos = datos,
                extraido = extraido,
                onComercioEditado = onComercioEditado,
                onFechaEditada = onFechaEditada,
                onTotalEditado = onTotalEditado,
                onCategoriaElegida = onCategoriaElegida,
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
                    .padding(horizontal = 24.dp)
                    .testTag("revision_error_guardado"),
            )
        }

        // Dock inferior (mockup 07): "Guardar" persiste el ticket.
        Button(
            onClick = onGuardar,
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)
                .fillMaxWidth()
                .height(48.dp)
                .testTag("revision_guardar"),
            shape = RoundedCornerShape(9999.dp),
            colors = ButtonDefaults.buttonColors(),
        ) {
            Text(text = "Guardar")
        }
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
    extraido: TicketExtraido,
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

            LineasSoloLectura(lineas = extraido.lineas)

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

/** Lineas en SOLO LECTURA (su edicion es `edicion-lineas`, otra feature). */
@Composable
private fun LineasSoloLectura(
    lineas: List<Linea>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().testTag("revision_lineas")) {
        Text(
            text = "L\u00edneas",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (lineas.isEmpty()) {
            Text(
                text = "\u2014",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            lineas.forEach { linea ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = linea.concepto,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${linea.cantidad}\u00d7 ${formatoImporte(linea.precioUnitario)}",
                        style = MaterialTheme.typography.bodyMedium.cifrasTabulares(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

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
