package com.robertomr.tally.test.unit

import com.robertomr.tally.domain.expenses.data.PhotoFileStore

/**
 * Doble compartido de [PhotoFileStore] (spec persistencia-room): registra las
 * rutas borradas y devuelve un resultado configurable.
 */
class FakePhotoFileStore(
    var deleteResult: Boolean = true,
) : PhotoFileStore {

    val deletedPaths = mutableListOf<String>()

    override fun delete(path: String): Boolean {
        deletedPaths += path
        return deleteResult
    }
}
