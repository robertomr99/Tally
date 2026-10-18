package com.robertomr.tally.feature.resumen

import androidx.compose.ui.graphics.Color
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.feature.common.colorCategoria
import kotlin.math.pow
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contraste WCAG de la paleta de categorías (spec grafica-donut, T5 / D1):
 * cada color debe llegar a 3:1 (WCAG 1.4.11) sobre la tarjeta
 * (`surfaceContainerLow` #EFF5F0) y sobre la pista (`surfaceContainerHighest`
 * #DEE4DF); el icono blanco sobre el color, a 3:1 como mínimo (icono grande).
 */
class CategoriaUiContrasteTest {

    private val tarjeta = Color(0xFFEFF5F0)
    private val pista = Color(0xFFDEE4DF)

    private fun lineal(c: Float): Double =
        if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

    private fun luminancia(c: Color): Double =
        0.2126 * lineal(c.red) + 0.7152 * lineal(c.green) + 0.0722 * lineal(c.blue)

    private fun contraste(a: Color, b: Color): Double {
        val la = luminancia(a)
        val lb = luminancia(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    @Test
    fun cadaColorSuperaTresAUnoSobreTarjetaYPista() {
        for (c in Categoria.entries) {
            val color = colorCategoria(c)
            val sobreTarjeta = contraste(color, tarjeta)
            val sobrePista = contraste(color, pista)
            println("contraste $c: tarjeta=%.2f pista=%.2f blanco=%.2f".format(sobreTarjeta, sobrePista, contraste(color, Color.White)))
            assertTrue("$c sobre tarjeta $sobreTarjeta", sobreTarjeta >= 3.0)
            assertTrue("$c sobre pista $sobrePista", sobrePista >= 3.0)
            assertTrue("$c con icono blanco", contraste(color, Color.White) >= 3.0)
        }
    }

    @Test
    fun losSeisColoresSonDistintos() {
        assertTrue(Categoria.entries.map { colorCategoria(it) }.toSet().size == Categoria.entries.size)
    }
}
