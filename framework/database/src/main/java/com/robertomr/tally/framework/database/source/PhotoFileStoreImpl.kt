package com.robertomr.tally.framework.database.source

import android.content.Context
import com.robertomr.tally.domain.expenses.data.PhotoFileStore
import java.io.File

/**
 * Implementación de [PhotoFileStore] sobre el almacenamiento privado de la app
 * (interna de `:framework:database`; AGENTS.md).
 *
 * La foto se guarda en `filesDir` y la ruta queda en Room: si la ruta es
 * absoluta se usa tal cual y si es relativa se resuelve bajo `filesDir`. El
 * borrado es best-effort: un fichero que ya no existe no es un error.
 */
internal class PhotoFileStoreImpl(
    private val context: Context,
) : PhotoFileStore {

    override fun delete(path: String): Boolean {
        if (path.isBlank()) return false
        val file = File(path)
        val target = if (file.isAbsolute) file else File(context.filesDir, path)
        return target.exists() && target.delete()
    }
}
