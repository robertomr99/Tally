package com.robertomr.tally.test.unit

import com.robertomr.tally.domain.expenses.data.PhotoFileStore

/**
 * Doble compartido de [PhotoFileStore] (spec persistencia-room/captura-foto/
 * ocr-mlkit): registra las rutas borradas y leidas y los JPEG guardados, con
 * resultados configurables.
 */
class FakePhotoFileStore(
    var deleteResult: Boolean = true,
    var savedName: String = "fotos/captura_fake.jpg",
    var saveError: Throwable? = null,
    /** Payloads por ruta relativa para [read]; una ruta sin entrada -> `null`. */
    var readPayloads: Map<String, ByteArray> = emptyMap(),
    var readError: Throwable? = null,
) : PhotoFileStore {

    val deletedPaths = mutableListOf<String>()
    val savedPayloads = mutableListOf<ByteArray>()
    val readPaths = mutableListOf<String>()

    /** Bytes guardados por nombre devuelto (round-trip save -> read). */
    private val savedByName = mutableMapOf<String, ByteArray>()

    override suspend fun save(jpeg: ByteArray): String {
        saveError?.let { throw it }
        savedPayloads += jpeg
        savedByName[savedName] = jpeg
        return savedName
    }

    override suspend fun read(path: String): ByteArray? {
        readPaths += path
        readError?.let { throw it }
        // Round-trip: si no hay payload explicito, devuelve lo guardado con save
        // (asi el OCR consume los bytes reales que el fake escribio).
        return readPayloads[path] ?: savedByName[path]
    }

    override fun delete(path: String): Boolean {
        deletedPaths += path
        return deleteResult
    }
}
