package com.robertomr.tally.test.unit

import com.robertomr.tally.domain.expenses.data.PhotoFileStore

/**
 * Doble compartido de [PhotoFileStore] (spec persistencia-room/captura-foto):
 * registra las rutas borradas y los JPEG guardados, con resultados
 * configurables.
 */
class FakePhotoFileStore(
    var deleteResult: Boolean = true,
    var savedName: String = "fotos/captura_fake.jpg",
    var saveError: Throwable? = null,
) : PhotoFileStore {

    val deletedPaths = mutableListOf<String>()
    val savedPayloads = mutableListOf<ByteArray>()

    override suspend fun save(jpeg: ByteArray): String {
        saveError?.let { throw it }
        savedPayloads += jpeg
        return savedName
    }

    override fun delete(path: String): Boolean {
        deletedPaths += path
        return deleteResult
    }
}
