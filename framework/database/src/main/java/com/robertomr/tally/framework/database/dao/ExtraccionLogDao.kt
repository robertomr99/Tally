package com.robertomr.tally.framework.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.robertomr.tally.framework.database.entity.ExtraccionLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO del log de extracciones (spec log-extracciones). Las lecturas devuelven
 * [Flow]; insertar + recortar va en una sola `@Transaction` (retención).
 */
@Dao
interface ExtraccionLogDao {

    @Insert
    suspend fun insertar(fila: ExtraccionLogEntity): Long

    /** Borra todo salvo las [maximo] filas más recientes (empate por `id`). */
    @Query(
        "DELETE FROM extracciones_log WHERE id NOT IN (" +
            "SELECT id FROM extracciones_log ORDER BY marca_tiempo DESC, id DESC LIMIT :maximo)"
    )
    suspend fun recortar(maximo: Int)

    @Transaction
    suspend fun insertarYRecortar(fila: ExtraccionLogEntity, maximo: Int) {
        insertar(fila)
        recortar(maximo)
    }

    @Query("SELECT * FROM extracciones_log ORDER BY marca_tiempo DESC, id DESC LIMIT :limite")
    fun observarRecientes(limite: Int): Flow<List<ExtraccionLogEntity>>

    @Query("DELETE FROM extracciones_log")
    suspend fun borrarTodo()
}
