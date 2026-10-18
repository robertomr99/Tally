package com.robertomr.tally.feature.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.robertomr.tally.domain.expenses.entities.Categoria

/**
 * Presentación de [Categoria] compartida entre pantallas (spec grafica-donut,
 * D2): etiqueta en español, icono y color fijo. El color va por categoría, no
 * por posición, para que sea el mismo en gráfica, chips y listado.
 */
fun etiquetaCategoria(categoria: Categoria): String = when (categoria) {
    Categoria.ALIMENTACION -> "Alimentación"
    Categoria.HOGAR -> "Hogar"
    Categoria.TRANSPORTE -> "Transporte"
    Categoria.OCIO -> "Ocio"
    Categoria.SALUD -> "Salud"
    Categoria.OTROS -> "Otros"
}

fun iconoCategoria(categoria: Categoria): ImageVector = when (categoria) {
    Categoria.ALIMENTACION -> Icons.Outlined.Restaurant
    Categoria.HOGAR -> Icons.Outlined.Home
    Categoria.TRANSPORTE -> Icons.Outlined.DirectionsCar
    Categoria.OCIO -> Icons.Outlined.SportsEsports
    Categoria.SALUD -> Icons.Outlined.LocalHospital
    Categoria.OTROS -> Icons.Outlined.Category
}

/**
 * Paleta semántica apagada (DESIGN.md, Open Design Question #1 resuelta). El
 * ámbar se oscurece de #C87D32 a #B86A22 para llegar a 3:1 sobre
 * `surfaceContainerLow` (WCAG 1.4.11); el resto son los valores del design
 * system. El contraste se comprueba en `CategoriaUiContrasteTest`.
 */
fun colorCategoria(categoria: Categoria): Color = when (categoria) {
    Categoria.ALIMENTACION -> Color(0xFFB86A22) // ámbar
    Categoria.TRANSPORTE -> Color(0xFF4A6B82) // azul acero
    Categoria.OCIO -> Color(0xFF7A587A) // ciruela
    Categoria.HOGAR -> Color(0xFF5D6E3F) // musgo
    Categoria.SALUD -> Color(0xFF2E776E) // verde azulado
    Categoria.OTROS -> Color(0xFF5F6561) // pizarra
}
