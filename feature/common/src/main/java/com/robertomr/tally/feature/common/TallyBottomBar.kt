package com.robertomr.tally.feature.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val PillShape = RoundedCornerShape(9999.dp)

// Destinos de la bottom bar (mockup 02). Vive en :feature:common y :app lo
// mapea 1:1 con sus rutas; aquí nunca hay NavController, solo callbacks.
enum class BottomBarDestino(
    val icon: ImageVector,
    val label: String,
    val testTag: String,
) {
    Resumen(
        icon = Icons.Filled.Insights,
        label = "Resumen",
        testTag = "bottom_bar_resumen",
    ),
    Gastos(
        icon = Icons.Filled.ReceiptLong,
        label = "Gastos",
        testTag = "bottom_bar_gastos",
    ),
    Escanear(
        icon = Icons.Filled.DocumentScanner,
        label = "Escanear",
        testTag = "bottom_bar_escanear",
    ),
}

// Bottom bar de 3 destinos del mockup 02: fila de 80dp sobre
// surface-container-low; item activo con icono en pill secondary-container /
// on-secondary-container y label semibold en primary; inactivos en
// on-surface-variant. Área táctil de cada item >= 44dp.
@Composable
fun TallyBottomBar(
    activeDestination: BottomBarDestino,
    onNavigate: (BottomBarDestino) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .navigationBarsPadding(),
    ) {
        // El fondo y el borde llegan a todo el ancho; los items, agrupados en la columna.
        ColumnaCentrada {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    // Los 3 destinos forman un grupo de selección único para TalkBack (V18).
                    .selectableGroup()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BottomBarDestino.entries.forEach { destination ->
                    TallyBottomBarItem(
                        destination = destination,
                        active = destination == activeDestination,
                        onClick = { onNavigate(destination) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TallyBottomBarItem(
    destination: BottomBarDestino,
    active: Boolean,
    onClick: () -> Unit,
) {
    val pillBackground =
        if (active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val iconTint =
        if (active) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant
    val labelColor =
        if (active) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .testTag(destination.testTag)
            .selectable(
                selected = active,
                role = Role.Tab,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(PillShape)
                .background(pillBackground)
                .padding(horizontal = 20.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = destination.icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            color = labelColor,
        )
    }
}
