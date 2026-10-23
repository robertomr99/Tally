package com.robertomr.tally.feature.resumen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.robertomr.tally.domain.expenses.entities.Ticket
import com.robertomr.tally.feature.common.FilaTicket
import com.robertomr.tally.feature.common.TituloSeccion
import com.robertomr.tally.feature.common.formaFilaTicket
import java.time.LocalDate

/**
 * Vista previa de los últimos tickets del período bajo el donut (spec
 * listado-gastos). Va dentro de la `Column` con scroll de Resumen: filas con un
 * bucle simple, sin lista perezosa anidada. `recientes == null` = cargando
 * (solo el título). Con más tickets de los mostrados aparece "Ver todos (N)".
 */
@Composable
internal fun SeccionRecientes(
    recientes: List<Ticket>?,
    totalTickets: Int,
    hoy: LocalDate,
    onTicketClick: (Long) -> Unit,
    onVerTodos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("resumen_recientes"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TituloSeccion("Gastos del periodo", modifier = Modifier.padding(top = 4.dp))
        when {
            recientes == null -> Unit
            recientes.isEmpty() -> Text(
                text = "Sin tickets en este periodo.",
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .testTag("resumen_recientes_vacio"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> {
                Column {
                    recientes.forEachIndexed { indice, ticket ->
                        FilaTicket(
                            ticket = ticket,
                            tag = "resumen_ticket_${ticket.id}",
                            onClick = onTicketClick,
                            forma = formaFilaTicket(indice == 0, indice == recientes.lastIndex),
                            conSeparador = indice > 0,
                            mostrarFechaCorta = true,
                            hoy = hoy,
                        )
                    }
                }
                if (totalTickets > recientes.size) {
                    TextButton(
                        onClick = onVerTodos,
                        modifier = Modifier.testTag("boton_ver_todos"),
                    ) {
                        Text("Ver todos ($totalTickets)")
                    }
                }
            }
        }
    }
}
