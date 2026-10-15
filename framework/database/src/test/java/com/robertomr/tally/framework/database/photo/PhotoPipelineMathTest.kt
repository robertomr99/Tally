package com.robertomr.tally.framework.database.photo

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests JVM de la matematica del pipeline de foto (spec captura-foto, T7):
 * tablas de escalado (~1280 px lado largo, proporcion conservada) y mapeo
 * EXIF -> grados.
 */
class PhotoPipelineMathTest {

    // --- Escalado: <= 1280 px no escala; > 1280 escala conservando proporcion ---

    @Test
    fun dimensionsAtOrBelowLimitAreNotScaled() {
        assertEquals(1280 to 960, PhotoPipelineMath.dimensionesObjetivo(1280, 960))
        assertEquals(960 to 1280, PhotoPipelineMath.dimensionesObjetivo(960, 1280))
        assertEquals(1280 to 1280, PhotoPipelineMath.dimensionesObjetivo(1280, 1280))
        assertEquals(800 to 600, PhotoPipelineMath.dimensionesObjetivo(800, 600))
        assertEquals(1 to 1, PhotoPipelineMath.dimensionesObjetivo(1, 1))
    }

    @Test
    fun dimensionsAboveLimitScaleLongSideTo1280KeepingRatio() {
        assertEquals(1280 to 640, PhotoPipelineMath.dimensionesObjetivo(2000, 1000))
        assertEquals(640 to 1280, PhotoPipelineMath.dimensionesObjetivo(1000, 2000))
        assertEquals(1280 to 960, PhotoPipelineMath.dimensionesObjetivo(4000, 3000))
        assertEquals(960 to 1280, PhotoPipelineMath.dimensionesObjetivo(3000, 4000))
    }

    @Test
    fun scaledLongSideIsExactlyTheLimitAndProportionIsPreserved() {
        val (ancho, alto) = PhotoPipelineMath.dimensionesObjetivo(1920, 1080)

        assertEquals(1280, maxOf(ancho, alto))
        // 1920/1080 = 1.777...; 1280/720 = 1.777...
        assertEquals(1920.0 / 1080.0, ancho.toDouble() / alto.toDouble(), 0.01)
    }

    // --- inSampleSize: potencia de 2 que no baja del objetivo ---

    @Test
    fun sampleSizeIsPowerOfTwoThatDoesNotUndershootTarget() {
        assertEquals(1, PhotoPipelineMath.inSampleSize(1280, 960, 1280, 960))
        assertEquals(1, PhotoPipelineMath.inSampleSize(2000, 1000, 1280, 640))
        assertEquals(2, PhotoPipelineMath.inSampleSize(4000, 3000, 1280, 960))
        assertEquals(4, PhotoPipelineMath.inSampleSize(8000, 6000, 1280, 960))
    }

    // --- Orientacion EXIF (tag 0x0112) -> grados de rotacion CW ---

    @Test
    fun exifOrientationMapsToClockwiseDegrees() {
        assertEquals(0, PhotoPipelineMath.gradosParaOrientacion(1)) // normal
        assertEquals(180, PhotoPipelineMath.gradosParaOrientacion(3))
        assertEquals(90, PhotoPipelineMath.gradosParaOrientacion(6))
        assertEquals(270, PhotoPipelineMath.gradosParaOrientacion(8))
    }

    @Test
    fun mirroredAndUnknownOrientationsFallBackToZeroDegrees() {
        // Espejos (2/4/5/7) sin corregir en este MVP; desconocidos -> 0.
        assertEquals(0, PhotoPipelineMath.gradosParaOrientacion(2))
        assertEquals(0, PhotoPipelineMath.gradosParaOrientacion(4))
        assertEquals(0, PhotoPipelineMath.gradosParaOrientacion(5))
        assertEquals(0, PhotoPipelineMath.gradosParaOrientacion(7))
        assertEquals(0, PhotoPipelineMath.gradosParaOrientacion(0))
        assertEquals(0, PhotoPipelineMath.gradosParaOrientacion(42))
    }
}
