package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.data.ExtractorPorMotor
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.OfertaMotor
import javax.inject.Inject

/** Motor alternativo a [Motor] `actual` y su disponibilidad (spec reintento-otro-motor, D1). */
class ObtenerOtroMotor @Inject constructor(
    private val extractor: ExtractorPorMotor,
) {
    operator fun invoke(actual: Motor): OfertaMotor {
        val otro = when (actual) {
            Motor.LOCAL -> Motor.NUBE
            Motor.NUBE -> Motor.LOCAL
        }
        return OfertaMotor(otro, extractor.disponibilidad(otro))
    }
}
