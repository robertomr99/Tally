package com.robertomr.tally.feature.gastos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

// Placeholder navegable de la feature navegacion-base. El contenido real llega
// con listado-gastos; sin ViewModel ni estado propio (regla del spec).
@Composable
fun GastosScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .testTag("pantalla_gastos"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Gastos",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
