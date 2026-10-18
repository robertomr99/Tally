package com.robertomr.tally.feature.resumen

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.foundation.rememberScrollState

/** Estado puramente visual de Resumen (AGENTS.md): scroll de la columna. */
@Stable
class ResumenState(val scroll: ScrollState)

@Composable
fun rememberResumenState(): ResumenState {
    val scroll = rememberScrollState()
    return androidx.compose.runtime.remember(scroll) { ResumenState(scroll) }
}
