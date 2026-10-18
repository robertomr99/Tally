package com.robertomr.tally.framework.ai.modelo

import android.content.Context
import com.robertomr.tally.domain.expenses.data.ModelAccessTokenStore
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Token de Hugging Face del usuario en un fichero de `noBackupFilesDir`
 * (almacenamiento privado y excluido de las copias de seguridad; spec
 * descarga-modelo, D4). El valor nunca se expone ni se escribe en logs.
 */
internal class FileModelAccessTokenStore(context: Context) : ModelAccessTokenStore {

    private val fichero = File(context.noBackupFilesDir, NOMBRE)
    private val hay = MutableStateFlow(fichero.isFile && fichero.length() > 0)

    override val hayToken: Flow<Boolean> = hay

    override fun guardar(token: String) {
        fichero.writeText(token.trim(), Charsets.UTF_8)
        hay.value = true
    }

    override fun borrar() {
        fichero.delete()
        hay.value = false
    }

    /** Lectura para el worker; `null` si no hay token. */
    fun leer(): String? =
        if (fichero.isFile) fichero.readText(Charsets.UTF_8).trim().ifEmpty { null } else null

    private companion object {
        const val NOMBRE = "hf_access_token"
    }
}
