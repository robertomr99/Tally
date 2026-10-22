package com.robertomr.tally.feature.escanear

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.FormaPago
import com.robertomr.tally.domain.expenses.entities.DatosRevision
import com.robertomr.tally.domain.expenses.entities.TicketExtraido
import com.robertomr.tally.feature.common.BadgeMotor
import com.robertomr.tally.feature.common.CabeceraPantalla
import com.robertomr.tally.feature.common.ColumnaCentrada
import com.robertomr.tally.feature.common.DockEdicion
import com.robertomr.tally.feature.common.FormularioTicketEditable
import com.robertomr.tally.feature.common.PADDING_PANTALLA

/**
 * Pantalla de revision y edicion del ticket digitalizado (spec revision-datos,
 * mockup 07; spec edicion-lineas, mockup 08; adaptados a DESIGN.md). Sobre la
 * extraccion tal cual salio del motor, el usuario corrige la cabecera
 * (comercio, fecha, total y categoria con chips de la enum fija de 6) y, en el
 * editor de lineas, anade, elimina y edita concepto/cantidad/precio.
 *
 * Composicion fina (spec detalle-ticket, D1): el formulario editable, el dock
 * y el badge de motor viven en `:feature:common` porque tambien los usa el
 * detalle del ticket guardado; aqui solo se ensamblan con el estado del
 * flujo de escaneo. Los testTags `revision_*` no cambian.
 *
 * Decoracion ficticia PROHIBIDA (DESIGN.md Do's and Don'ts): NIF, direccion,
 * IVA desglosado, hash, "% verificado", etc. El badge de motor es real.
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
    onFormaPagoElegida: (FormaPago) -> Unit,
    onEntregadoEditado: (String) -> Unit,
    onTotalAutomaticoElegido: (Boolean) -> Unit,
    onDescartar: () -> Unit,
    onGuardar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .imePadding()
            .testTag("revision_pantalla"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ColumnaCentrada { CabeceraPantalla(titulo = "Revisar ticket") }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            // El scroll ocupa todo el ancho; el formulario va en la columna centrada.
            ColumnaCentrada {
                Column(
                    modifier = Modifier.padding(horizontal = PADDING_PANTALLA),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // El badge se desplaza con el contenido: no gasta altura fija (V21).
                    BadgeMotor(
                        motor = extraido.motor,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 4.dp),
                    )
                    FormularioTicketEditable(
                        datos = datos,
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
                        onTotalAutomaticoElegido = onTotalAutomaticoElegido,
                    )
                }
            }
        }

        if (errorGuardado != null) {
            Text(
                text = errorGuardado,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PADDING_PANTALLA, vertical = 4.dp)
                    .testTag("revision_error_guardado"),
            )
        }

        DockEdicion(
            onDescartar = onDescartar,
            onGuardar = onGuardar,
        )
    }
}
