package com.example.app.data

import com.example.app.core.database.NoteDao
import com.example.app.core.database.NoteEntity
import com.example.app.core.network.NoteApiService
import com.example.app.core.sync.SyncTrigger
import com.example.app.domain.model.Note
import com.example.app.domain.model.SyncStatus
import com.example.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementacion offline-first del repositorio.
 *
 * Flujo de escritura:
 *   1. La UI llama addNote/deleteNote.
 *   2. Se persiste en Room con syncStatus = PENDING.
 *   3. Se notifica a SyncScheduler para encolar un SyncWorker.
 *   4. El Worker correra cuando haya conectividad y marcara como SYNCED.
 *
 * Flujo de lectura:
 *   - getNotes() lee SIEMPRE de Room: la UI funciona sin red y sin
 *     latencia perceptible.
 *
 * Flujo de refresh (LWW):
 *   - refreshFromServer() pide al backend la lista actual.
 *   - Para cada nota remota, si NO existe local o si el remoto tiene un
 *     updatedAt mayor que el local, el remoto gana (se upsert con
 *     syncStatus = CONFLICT cuando habia version local distinta, o
 *     SYNCED cuando no existia).
 *   - Si el local tiene mayor updatedAt, no se toca: ya esta en cola
 *     PENDING y el Worker lo subira en el proximo ciclo.
 */
@Singleton
class OfflineFirstNoteRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val noteApiService: NoteApiService,
    private val syncTrigger: SyncTrigger,
) : NoteRepository {

    override fun getNotes(): Flow<List<Note>> =
        noteDao.observeAll().map { list -> list.map(NoteEntity::toDomain) }

    override suspend fun addNote(note: Note) {
        // Escritura local primero (offline-first). El timestamp se renueva
        // para que LWW lo compare correctamente con la copia del servidor.
        noteDao.upsert(
            NoteEntity(
                id = note.id,
                title = note.title,
                content = note.content,
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING,
            )
        )
        syncTrigger.schedule()
    }

    override suspend fun deleteNote(id: String) {
        noteDao.deleteById(id)
        try {
            noteApiService.deleteNote(id)
        } catch (_: Exception) {
            // Sin red: el borrado solo permanece en local. En produccion
            // se modelaria como tombstone (deletedAt) y el Worker lo
            // propagaria; aqui se simplifica.
        }
    }

    override suspend fun refreshFromServer() {
        val remoteNotes = noteApiService.getAllNotes()
        remoteNotes.forEach { remote ->
            val local = noteDao.findById(remote.id)
            val remoteIsNewer = local == null || remote.updatedAt > local.updatedAt

            if (remoteIsNewer) {
                val status =
                    if (local != null && local.updatedAt != remote.updatedAt) SyncStatus.CONFLICT
                    else SyncStatus.SYNCED

                noteDao.upsert(
                    NoteEntity(
                        id = remote.id,
                        title = remote.title,
                        content = remote.content,
                        updatedAt = remote.updatedAt,
                        syncStatus = status,
                    )
                )
            }
            // local.updatedAt > remote.updatedAt: el local sigue en cola.
        }
    }
}

private fun NoteEntity.toDomain(): Note = Note(
    id = id,
    title = title,
    content = content,
    updatedAt = updatedAt,
    syncStatus = syncStatus,
)
