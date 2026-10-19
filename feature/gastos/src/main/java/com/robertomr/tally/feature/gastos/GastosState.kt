package com.robertomr.tally.feature.gastos

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember

/**
 * Estado puramente visual de Gastos (AGENTS.md): posición de scroll de la lista.
 * `rememberLazyListState` es saveable, así que sobrevive a la navegación al
 * detalle y a la rotación.
 */
@Stable
class GastosState(val lista: LazyListState)

@Composable
fun rememberGastosState(): GastosState {
    val lista = rememberLazyListState()
    return remember(lista) { GastosState(lista) }
}
