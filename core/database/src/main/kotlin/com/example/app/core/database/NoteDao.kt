package com.example.app.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * DAO de notas. observeAll() expone un Flow que reactivamente
 * notifica cualquier insercion/borrado/actualizacion -- la UI no
 * necesita refrescar manualmente.
 */
@Dao
interface NoteDao {

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE syncStatus = 'PENDING'")
    suspend fun getPending(): List<NoteEntity>

    @Upsert
    suspend fun upsert(note: NoteEntity)

    @Query("UPDATE notes SET syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE notes SET syncStatus = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: com.example.app.domain.model.SyncStatus)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: String)
}
