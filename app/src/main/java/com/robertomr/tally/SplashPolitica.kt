package com.robertomr.tally

import android.os.Build

/**
 * Politica del splash (spec splash-animado D3/D4/D5). Logica pura, sin Android,
 * para poder probarla en JVM. El splash NO espera a Hilt, Room ni OCR: solo se
 * mantiene un instante para que se vea la animacion del emblema.
 */
object SplashPolitica {

    /** Hold artificial maximo, en milisegundos. */
    const val SPLASH_MIN_MS = 700L

    /**
     * Primera creacion de MainActivity en este proceso. Se marca al arrancar para
     * que la rotacion, la vuelta desde recientes y el relanzado en caliente no
     * vuelvan a retener el splash.
     */
    @Volatile
    var yaMostrado: Boolean = false

    /**
     * @param esPrimeraCreacion true solo en arranque en frio (sin estado guardado y
     * primera actividad del proceso).
     * @param sdkInt version de Android; por debajo de 31 la compat no anima, hold 0.
     * @param escalaAnimacion valor de animator_duration_scale; 0 = reducir animaciones.
     * @param transcurridoMs milisegundos desde el inicio de onCreate.
     */
    fun debeMantener(
        esPrimeraCreacion: Boolean,
        sdkInt: Int,
        escalaAnimacion: Float,
        transcurridoMs: Long,
    ): Boolean {
        if (!esPrimeraCreacion) return false
        if (sdkInt < Build.VERSION_CODES.S) return false
        if (escalaAnimacion <= 0f) return false
        return transcurridoMs < SPLASH_MIN_MS
    }
}
