package com.robertomr.tally.framework.database.source

import android.content.Context
import com.robertomr.tally.domain.expenses.data.PhotoFileStore
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementación de [PhotoFileStore] sobre el almacenamiento privado de la app
 * (interna de `:framework:database`; AGENTS.md).
 *
 * La foto se guarda en `filesDir` y la ruta queda en Room: si la ruta es
 * absoluta se usa tal cual y si es relativa se resuelve bajo `filesDir`. El
 * borrado es best-effort: un fichero que ya no existe no es un error.
 *
 * Al guardar (spec captura-foto) SIEMPRE se devuelve un nombre relativo bajo
 * `filesDir/fotos/`; nunca una ruta absoluta.
 */
internal class PhotoFileStoreImpl(
    private val context: Context,
) : PhotoFileStore {

    override suspend fun save(jpeg: ByteArray): String = withContext(Dispatchers.IO) {
        val carpeta = File(context.filesDir, CARPETA_FOTOS).apply { mkdirs() }
        val nombreFichero = "captura_${UUID.randomUUID()}.jpg"
        File(carpeta, nombreFichero).writeBytes(jpeg)
        "$CARPETA_FOTOS/$nombreFichero"
    }

    override suspend fun read(path: String): ByteArray? = withContext(Dispatchers.IO) {
        if (path.isBlank()) return@withContext null
        val file = File(path)
        val target = if (file.isAbsolute) file else File(context.filesDir, path)
        if (target.isFile) target.readBytes() else null
    }

    override fun delete(path: String): Boolean {
        if (path.isBlank()) return false
        val file = File(path)
        val target = if (file.isAbsolute) file else File(context.filesDir, path)
        return target.exists() && target.delete()
    }

    private companion object {
        const val CARPETA_FOTOS = "fotos"
    }
}
