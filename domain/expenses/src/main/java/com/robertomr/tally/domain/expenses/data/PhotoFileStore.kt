package com.robertomr.tally.domain.expenses.data

/**
 * Almacenamiento del fichero de la foto original (interfaz de dominio; la
 * implementación sobre `filesDir` vive en `:framework:database`).
 *
 * La foto es evidencia inmutable del ticket: solo se borra junto con él
 * (CONTEXT.md). El borrado es best-effort: un fichero que ya no existe no es
 * un error.
 */
interface PhotoFileStore {

    /**
     * Guarda los bytes JPEG ya procesados de la foto y devuelve el NOMBRE
     * RELATIVO bajo el almacenamiento privado (p. ej. `fotos/captura_<uuid>.jpg`).
     *
     * Contrato de seguridad (spec captura-foto): la ruta devuelta SIEMPRE es
     * relativa a `filesDir`; nunca una ruta absoluta. La foto se guarda en el
     * momento de la captura, de modo que sobrevive a la muerte de la app.
     *
     * @return nombre relativo del fichero guardado.
     */
    suspend fun save(jpeg: ByteArray): String

    /**
     * Borra el fichero de la foto.
     *
     * @return true si se borró un fichero; false si no existía, la ruta estaba
     * vacía o el borrado no fue posible.
     */
    fun delete(path: String): Boolean
}
