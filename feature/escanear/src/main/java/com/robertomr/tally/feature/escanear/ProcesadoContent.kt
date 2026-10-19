package com.robertomr.tally.feature.escanear

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.DisponibilidadMotor
import com.robertomr.tally.domain.expenses.entities.Linea
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.OfertaMotor
import com.robertomr.tally.domain.expenses.entities.TicketExtraido
import com.robertomr.tally.feature.common.FilaRecibo
import com.robertomr.tally.feature.common.PapelTicket
import com.robertomr.tally.feature.common.SeparadorPunteado
import com.robertomr.tally.feature.common.importeDeLinea
import com.robertomr.tally.feature.common.reciboTexto
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Pantalla de procesado (mockup 06, spec procesado-ui T3-T5) como subflujo
 * interno de Escanear: pasos reales Encuadre -> OCR -> Categorizacion, badge
 * del motor REAL, silueta de ticket con scan beam y progreso circular SIN
 * porcentaje (CONTEXT.md: nunca % de confianza ni de progreso inventados).
 *
 * Tres estados comparten la raiz `procesado_pantalla`:
 * - [ProcesandoExtraccionContent]: extraccion en curso + "Cancelar".
 * - [ExtraccionListaContent]: resultado de SOLO LECTURA del `TicketExtraido`
 *   con completitud de campos y "Revisar ticket" deshabilitado (hasta
 *   `revision-datos`).
 * - [ExtraccionFallidaContent]: error honesto (copy en espanol, paleta
 *   `error`) con "Reintentar" (mismo motor) y "Volver a la foto".
 *
 * Decoracion ficticia PROHIBIDA (DESIGN.md R4): "Motor OCR v4.2", "Tally
 * Vision", "% verificado", "[ORG_NAME 98%]", "68%", "Garantia de privacidad
 * criptografica", candado, toggle "Cambiar a Nube/Dispositivo"
 * (= `reintento-otro-motor`) y mensajes de progreso simulados. Animaciones
 * permitidas (las del mockup 06): scan beam, radar y progreso circular.
 */

// ---------------------------------------------------------------------------
// Estados raiz
// ---------------------------------------------------------------------------

/**
 * Procesado en curso (mockup 06): badge del motor en estado procesando,
 * silueta de ticket con scan beam + radar, progreso circular indeterminado,
 * pasos reales y "Cancelar" (aborta la extraccion y conserva la foto).
 */
@Composable
fun ProcesandoExtraccionContent(
    motor: Motor,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .testTag("procesado_pantalla"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CabeceraDeProceso(motor = motor, estado = EstadoBadge.PROCESANDO)

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Analizando ticket de compra",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Extrayendo comercio, fecha e importes del ticket.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            SiluetaDeTicketConProgreso()
        }

        PasosDeProceso(categorizacion = EstadoPaso.ACTIVO)

        Spacer(modifier = Modifier.height(8.dp))
        BotonCancelar(
            onCancelar = onCancelar,
            modifier = Modifier.padding(bottom = 24.dp),
        )
    }
}

/**
 * Resultado de solo lectura (spec procesado-ui T4): los datos del
 * [TicketExtraido] tal cual salieron del motor + completitud de campos real +
 * "Revisar ticket", que abre la revision editable (spec revision-datos, mockup
 * 07). La salida de este estado es back del sistema (orden "Volver" al Borrador).
 */
@Composable
fun ExtraccionListaContent(
    extraido: TicketExtraido,
    otro: OfertaMotor,
    onRevisar: () -> Unit,
    onReintentarOtro: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .testTag("procesado_pantalla"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Tras el resultado el badge refleja el motor del propio resultado.
        CabeceraDeProceso(motor = extraido.motor, estado = EstadoBadge.COMPLETADO)
        PasosDeProceso(categorizacion = EstadoPaso.COMPLETADO)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            ResumenDeTicket(extraido = extraido)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = etiquetaCompletitud(extraido),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("procesado_completitud"),
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Reintento con el otro motor (spec reintento-otro-motor): secundario,
        // deshabilitado con motivo si el otro motor no esta disponible.
        BotonOtroMotor(
            otro = otro,
            onClick = onReintentarOtro,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp),
        )

        // "Revisar ticket" abre la revision editable (spec revision-datos).
        // Back del sistema desde el resultado vuelve a la foto.
        Button(
            onClick = onRevisar,
            enabled = true,
            modifier = Modifier
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp)
                .fillMaxWidth()
                .height(48.dp)
                .testTag("procesado_revisar"),
            shape = RoundedCornerShape(9999.dp),
            colors = ButtonDefaults.buttonColors(),
        ) {
            Text(text = "Revisar ticket")
        }
    }
}

/**
 * Error de extraccion/red (spec procesado-ui T5): el motivo real del fallo
 * (sin inventar datos ni cambiar de motor, ADR 0001) y las dos salidas
 * honestas: reintentar con el MISMO motor o volver a la foto (Borrador
 * intacto).
 */
@Composable
fun ExtraccionFallidaContent(
    mensaje: String,
    motor: Motor,
    otro: OfertaMotor,
    hayAnterior: Boolean,
    onReintentar: () -> Unit,
    onReintentarOtro: () -> Unit,
    onVolverALaFoto: () -> Unit,
    onVolverAnterior: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .testTag("procesado_pantalla"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CabeceraDeProceso(motor = motor, estado = EstadoBadge.FALLIDO)
        PasosDeProceso(categorizacion = EstadoPaso.FALLIDO)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No se pudieron extraer los datos",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = mensaje,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("procesado_error"),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "La foto y el texto del ticket siguen guardados.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        BotonOtroMotor(
            otro = otro,
            onClick = onReintentarOtro,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp),
        )
        if (hayAnterior) {
            TextButton(
                onClick = onVolverAnterior,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .testTag("procesado_volver_anterior"),
            ) {
                Text(text = "Volver al resultado anterior")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onVolverALaFoto,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("procesado_volver_foto"),
                shape = RoundedCornerShape(9999.dp),
            ) {
                Text(text = "Volver a la foto")
            }
            Button(
                onClick = onReintentar,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("procesado_reintentar"),
                shape = RoundedCornerShape(9999.dp),
                colors = ButtonDefaults.buttonColors(),
            ) {
                Text(text = "Reintentar")
            }
        }
    }
}

/**
 * "Probar con {otro}" (spec reintento-otro-motor, patron del mockup 07):
 * outlined secundario con icono de refrescar. Si el otro motor no esta
 * [DisponibilidadMotor.DISPONIBLE] se muestra deshabilitado con el motivo
 * (tag `procesado_reintentar_otro_motivo`), sin ocultar la razon.
 */
@Composable
private fun BotonOtroMotor(
    otro: OfertaMotor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val disponible = otro.disponibilidad == DisponibilidadMotor.DISPONIBLE
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OutlinedButton(
            onClick = onClick,
            enabled = disponible,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("procesado_reintentar_otro"),
            shape = RoundedCornerShape(9999.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Probar con ${etiquetaMotorCorta(otro.motor)}")
        }
        val motivo = motivoNoDisponible(otro.disponibilidad)
        if (motivo != null) {
            Text(
                text = motivo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .testTag("procesado_reintentar_otro_motivo"),
            )
        }
    }
}

/** Motivo visible de un motor no disponible; null si esta [DisponibilidadMotor.DISPONIBLE]. */
internal fun motivoNoDisponible(disponibilidad: DisponibilidadMotor): String? = when (disponibilidad) {
    DisponibilidadMotor.DISPONIBLE -> null
    DisponibilidadMotor.SIN_MODELO -> "Descarga el modelo local en Ajustes para usarlo"
    DisponibilidadMotor.NO_CAPAZ -> "Este dispositivo no puede ejecutar el motor local"
}

// ---------------------------------------------------------------------------
// Piezas del mockup 06
// ---------------------------------------------------------------------------

private enum class EstadoBadge { PROCESANDO, COMPLETADO, FALLIDO }
private enum class EstadoPaso { COMPLETADO, ACTIVO, PENDIENTE, FALLIDO }

/** Badge de motor (DESIGN.md, componente nuevo obligatorio) + etiqueta de proceso veraz. */
@Composable
private fun CabeceraDeProceso(
    motor: Motor,
    estado: EstadoBadge,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(9999.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
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
                modifier = Modifier.testTag("procesado_badge_motor"),
            )
            Spacer(modifier = Modifier.width(8.dp))
            when (estado) {
                EstadoBadge.PROCESANDO -> PuntoAnimado()
                EstadoBadge.COMPLETADO -> Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "Procesado",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.secondary,
                )
                EstadoBadge.FALLIDO -> Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Proceso fallido",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = etiquetaProceso(motor, estado),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.testTag("procesado_proceso"),
        )
    }
}

/** Punto pulsante del badge/chip activo (las animaciones del mockup 06). */
@Composable
private fun PuntoAnimado(modifier: Modifier = Modifier) {
    val transicion = rememberInfiniteTransition(label = "punto")
    val alpha by transicion.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )
    Box(
        modifier = modifier
            .size(8.dp)
            .graphicsLayer { this.alpha = alpha }
            .background(MaterialTheme.colorScheme.secondary, CircleShape),
    )
}

/**
 * Pasos reales del pipeline (U5): Encuadre y OCR ya estaban Completados cuando
 * se abre el procesado (la foto se guardo y el OCR corrio durante la captura);
 * Categorizacion es el paso que corre (`ExtraerTicket`). Sin secuencia falsa.
 */
@Composable
private fun PasosDeProceso(
    categorizacion: EstadoPaso,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // Aire respecto al texto "Procesado en ..." de la cabecera y al contenido de debajo.
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 12.dp)
            .testTag("procesado_pasos"),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChipPaso(
            texto = "Encuadre",
            estado = EstadoPaso.COMPLETADO,
            modifier = Modifier.testTag("procesado_paso_encuadre"),
        )
        IconoSeparador()
        ChipPaso(
            texto = "OCR",
            estado = EstadoPaso.COMPLETADO,
            modifier = Modifier.testTag("procesado_paso_ocr"),
        )
        IconoSeparador()
        ChipPaso(
            texto = "Categorizaci\u00f3n",
            estado = categorizacion,
            modifier = Modifier.testTag("procesado_paso_categoria"),
        )
    }
}

@Composable
private fun ChipPaso(
    texto: String,
    estado: EstadoPaso,
    modifier: Modifier = Modifier,
) {
    val contenedor = when (estado) {
        EstadoPaso.COMPLETADO -> MaterialTheme.colorScheme.secondaryContainer
        EstadoPaso.ACTIVO -> MaterialTheme.colorScheme.primary
        EstadoPaso.PENDIENTE -> MaterialTheme.colorScheme.surfaceContainerHigh
        EstadoPaso.FALLIDO -> MaterialTheme.colorScheme.errorContainer
    }
    val contenido = when (estado) {
        EstadoPaso.COMPLETADO -> MaterialTheme.colorScheme.onSecondaryContainer
        EstadoPaso.ACTIVO -> MaterialTheme.colorScheme.onPrimary
        EstadoPaso.PENDIENTE -> MaterialTheme.colorScheme.outline
        EstadoPaso.FALLIDO -> MaterialTheme.colorScheme.onErrorContainer
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(9999.dp))
            .background(contenedor)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (estado) {
            EstadoPaso.COMPLETADO -> Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = contenido,
            )
            EstadoPaso.ACTIVO -> Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(MaterialTheme.colorScheme.secondaryFixed, CircleShape),
            )
            EstadoPaso.FALLIDO -> Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = contenido,
            )
            EstadoPaso.PENDIENTE -> Unit
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = texto,
            style = MaterialTheme.typography.labelSmall,
            color = contenido,
        )
    }
}

@Composable
private fun IconoSeparador(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Outlined.ChevronRight,
        contentDescription = null,
        modifier = modifier.size(12.dp),
        tint = MaterialTheme.colorScheme.outlineVariant,
    )
}

/**
 * Silueta de ticket (mockup 06) SIN etiquetas ficticias ("[ORG_NAME 98%]",
 * "ITEM_01"): solo barras de esqueleto, scan beam y el HUD de progreso
 * circular INDETERMINADO (nunca % numerico).
 */
@Composable
private fun SiluetaDeTicketConProgreso(modifier: Modifier = Modifier) {
    val transicion = rememberInfiniteTransition(label = "procesado")
    val avanceBeam by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scanBeam",
    )
    val avanceRadar by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "radar",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .width(256.dp)
                .height(320.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.85f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    BarraDeEsqueleto(
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(12.dp),
                    )
                    BarraDeEsqueleto(
                        modifier = Modifier
                            .fillMaxWidth(0.45f)
                            .height(8.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHighest,
                            RoundedCornerShape(4.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            )
            Spacer(modifier = Modifier.height(12.dp))
            repeat(3) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BarraDeEsqueleto(
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    BarraDeEsqueleto(
                        modifier = Modifier
                            .width(32.dp)
                            .height(10.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(0.dp),
                    )
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BarraDeEsqueleto(
                        modifier = Modifier
                            .width(48.dp)
                            .height(8.dp),
                    )
                    BarraDeEsqueleto(
                        modifier = Modifier
                            .width(64.dp)
                            .height(8.dp),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(16.dp)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            RoundedCornerShape(4.dp),
                        ),
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
            ) {
                listOf(4.dp, 8.dp, 2.dp, 6.dp, 4.dp, 10.dp).forEach { ancho ->
                    Box(
                        modifier = Modifier
                            .width(ancho)
                            .height(12.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                RoundedCornerShape(1.dp),
                            ),
                    )
                }
            }
        }

        // Scan beam (linea de escaneo del mockup).
        Box(
            modifier = Modifier
                .width(256.dp)
                .height(320.dp)
                .clip(RoundedCornerShape(16.dp)),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(2.dp)
                    .graphicsLayer { translationY = avanceBeam * 320.dp.toPx() }
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(
                                androidx.compose.ui.graphics.Color.Transparent,
                                MaterialTheme.colorScheme.secondary,
                                androidx.compose.ui.graphics.Color.Transparent,
                            ),
                        ),
                    ),
            )
        }

        // Radar ambiental (dos ondas desfasadas, como en el mockup).
        listOf(0f to 0.4f, 0.4f to 0.25f).forEach { (desfase, maximoAlfa) ->
            val avance = (avanceRadar + desfase) % 1f
            Box(
                modifier = Modifier
                    .size(176.dp)
                    .graphicsLayer {
                        val escala = 0.75f + 0.25f * avance
                        scaleX = escala
                        scaleY = escala
                        alpha = (1f - avance) * maximoAlfa
                    }
                    .border(1.dp, MaterialTheme.colorScheme.secondary, CircleShape),
            )
        }

        // HUD de progreso circular INDETERMINADO (sin % de ningun tipo).
        Box(
            modifier = Modifier
                .size(144.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.95f))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(136.dp)
                    .testTag("procesado_progreso"),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                strokeWidth = 5.5.dp,
            )
            Text(
                text = "Extrayendo",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun BarraDeEsqueleto(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            RoundedCornerShape(4.dp),
        ),
    )
}

/** Boton "Cancelar" (mockup 06): texto, pill, >= 44dp. */
@Composable
private fun BotonCancelar(
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onCancelar,
        modifier = modifier
            .height(48.dp)
            .testTag("procesado_cancelar"),
        shape = RoundedCornerShape(9999.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "Cancelar")
    }
}

// ---------------------------------------------------------------------------
// Resultado de solo lectura
// ---------------------------------------------------------------------------

/**
 * Resumen del [TicketExtraido] tal cual (sin editar ni completar) como recibo
 * de papel (spec papel-ticket, D6): cabecera de comercio y fecha, categoria,
 * filas `n× CONCEPTO ..... importe` y total al final. Los campos sin dato
 * muestran "—" (nunca se inventa informacion). El importe de cada fila es
 * cantidad por precio unitario, un dato derivado.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResumenDeTicket(
    extraido: TicketExtraido,
    modifier: Modifier = Modifier,
) {
    PapelTicket(modifier = modifier) {
        Text(
            text = extraido.comercio.ifBlank { "—" },
            style = reciboTexto(MaterialTheme.typography.titleLarge)
                .copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = extraido.fechaEmision?.let(::formatoFecha) ?: "—",
            style = reciboTexto(MaterialTheme.typography.bodyMedium)
                .copy(textAlign = TextAlign.Center),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "Categoría: ${etiquetaCategoria(extraido.categoria)}",
            style = reciboTexto(MaterialTheme.typography.labelMedium)
                .copy(textAlign = TextAlign.Center),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )

        SeparadorPunteado(modifier = Modifier.padding(vertical = 16.dp))

        if (extraido.lineas.isEmpty()) {
            Text(
                text = "—",
                style = reciboTexto(MaterialTheme.typography.bodyMedium),
                color = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                extraido.lineas.forEach { linea ->
                    FilaRecibo(
                        cantidad = linea.cantidad,
                        concepto = linea.concepto,
                        importe = formatoImporte(importeDeLinea(linea.cantidad, linea.precioUnitario)),
                        detalle = if (linea.cantidad > 1) {
                            "${linea.cantidad} × ${formatoImporte(linea.precioUnitario)}"
                        } else {
                            null
                        },
                    )
                }
            }
        }

        SeparadorPunteado(modifier = Modifier.padding(vertical = 16.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Total (con IVA)",
                style = reciboTexto(MaterialTheme.typography.labelLarge)
                    .copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
            Text(
                text = extraido.total?.let(::formatoImporte) ?: "—",
                style = reciboTexto(MaterialTheme.typography.headlineSmall)
                    .copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Helpers puros (testables en JVM sin Compose)
// ---------------------------------------------------------------------------

/** Campos revisables del resultado: comercio, fecha, total, lineas y categoria. */
internal const val CAMPOS_REVISABLES = 5

/**
 * Completitud REAL de la extraccion (CONTEXT.md: estado, nunca % de
 * confianza): cuenta los [CAMPOS_REVISABLES] campos del [TicketExtraido] que
 * llevan dato extraido. La categoria solo cuenta si no es [Categoria.OTROS],
 * que es el valor por defecto del parser y del texto blank (con OCR vacio la
 * completitud es 0/5, escenario 5 del spec).
 */
internal fun camposExtraidos(extraido: TicketExtraido): Int {
    var extraidos = 0
    if (extraido.comercio.isNotBlank()) extraidos++
    if (extraido.fechaEmision != null) extraidos++
    if (extraido.total != null) extraidos++
    if (extraido.lineas.isNotEmpty()) extraidos++
    if (extraido.categoria != Categoria.OTROS) extraidos++
    return extraidos
}

/** Etiqueta "n/5 campos extraidos" sobre el [TicketExtraido]. */
internal fun etiquetaCompletitud(extraido: TicketExtraido): String =
    "${camposExtraidos(extraido)}/$CAMPOS_REVISABLES campos extra\u00eddos"

internal fun etiquetaMotor(motor: Motor): String = "Motor ${etiquetaMotorCorta(motor)}"

internal fun etiquetaMotorCorta(motor: Motor): String = when (motor) {
    Motor.NUBE -> "Nube \u00b7 Gemini"
    Motor.LOCAL -> "Local \u00b7 Gemma 3n"
}

private fun etiquetaProceso(motor: Motor, estado: EstadoBadge): String {
    val enNube = motor == Motor.NUBE
    val donde = if (enNube) "en la nube (Gemini)" else "en el dispositivo (Gemma 3n)"
    return when (estado) {
        EstadoBadge.PROCESANDO -> "Procesando $donde"
        EstadoBadge.COMPLETADO -> "Procesado $donde"
        EstadoBadge.FALLIDO -> "Proceso fallido $donde"
    }
}

internal fun etiquetaCategoria(categoria: Categoria): String = when (categoria) {
    Categoria.ALIMENTACION -> "Alimentaci\u00f3n"
    Categoria.HOGAR -> "Hogar"
    Categoria.TRANSPORTE -> "Transporte"
    Categoria.OCIO -> "Ocio"
    Categoria.SALUD -> "Salud"
    Categoria.OTROS -> "Otros"
}

internal fun formatoImporte(valor: Double): String =
    String.format(Locale.forLanguageTag("es-ES"), "%,.2f \u20ac", valor)

private val FORMATO_FECHA: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d/M/yyyy HH:mm")

private fun formatoFecha(fecha: LocalDateTime): String = fecha.format(FORMATO_FECHA)
