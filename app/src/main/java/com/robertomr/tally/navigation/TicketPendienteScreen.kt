package com.robertomr.tally.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

// TODO(detalle-ticket): destino mínimo del listado (spec listado-gastos, D2).
// detalle-ticket lo sustituye por la pantalla real manteniendo la ruta
// `ticket/{ticketId}`. Vive en :app porque los módulos feature no se conocen.
@Composable
internal fun TicketPendienteScreen(ticketId: Long, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(20.dp)
            .testTag("pantalla_detalle_ticket"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = "Detalle del ticket #$ticketId",
            modifier = Modifier.testTag("detalle_ticket_id"),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        TextButton(onClick = onBack, modifier = Modifier.testTag("boton_atras_detalle")) {
            Text("Atrás")
        }
    }
}
