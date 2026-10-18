package com.robertomr.tally.framework.ai.modelo

import android.content.Context
import java.io.File

/**
 * Ruta estable del modelo local (spec descarga-modelo, D6):
 * `getExternalFilesDir(null)/models/` (permite `adb push` de desarrollo); si no
 * hay almacenamiento externo, `filesDir/models`. `extraccion-local` consumira la
 * ruta via `ModelDownloader.rutaModelo()`.
 */
internal object ModelPaths {
    const val NOMBRE_ARCHIVO = "gemma-3n-E2B-it-int4.litertlm"

    /**
     * Fuente del modelo (gated: exige licencia aceptada y token del usuario).
     * No es un secreto.
     */
    const val URL_MODELO =
        "https://huggingface.co/google/gemma-3n-E2B-it-litert-lm/resolve/main/$NOMBRE_ARCHIVO"

    fun directorio(context: Context): File {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        return File(base, "models")
    }

    fun archivoFinal(context: Context): File = File(directorio(context), NOMBRE_ARCHIVO)

    fun archivoParcial(context: Context): File = File(archivoFinal(context).path + ".part")
}
