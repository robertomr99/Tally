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
     * Borra el fichero de la foto.
     *
     * @return true si se borró un fichero; false si no existía, la ruta estaba
     * vacía o el borrado no fue posible.
     */
    fun delete(path: String): Boolean
}
