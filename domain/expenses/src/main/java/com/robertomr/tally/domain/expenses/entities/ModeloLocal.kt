package com.robertomr.tally.domain.expenses.entities

/** Constantes del modelo local validado en el spike (spec descarga-modelo). */
object ModeloLocal {
    /** `gemma-3n-E2B-it-int4.litertlm`: 3.655.827.456 bytes (3,4 GB). */
    const val TAMANO_ESPERADO_BYTES: Long = 3_655_827_456L

    /** Margen de espacio libre que se exige ademas del resto por descargar. */
    const val MARGEN_ESPACIO_BYTES: Long = 500L * 1024 * 1024
}
