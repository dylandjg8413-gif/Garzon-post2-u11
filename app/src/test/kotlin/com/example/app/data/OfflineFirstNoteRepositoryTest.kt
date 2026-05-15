package com.example.app.data

import com.example.app.core.database.NoteDao
import com.example.app.core.database.NoteEntity
import com.example.app.core.network.NoteApiService
import com.example.app.core.network.NoteDto
import com.example.app.core.sync.SyncTrigger
import com.example.app.domain.model.SyncStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pruebas del algoritmo Last-Write-Wins.
 *
 * El repositorio depende de SyncTrigger (interfaz) en lugar de
 * SyncScheduler (impl de WorkManager) para poder usar un fake puro.
 * Ejecuta refreshFromServer en escenarios de timestamp distintos y
 * verifica el resultado en el DAO fake.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OfflineFirstNoteRepositoryTest {

    @Test
    fun `remote with newer timestamp overrides local`() = runTest {
        val dao = FakeNoteDao()
        dao.upsert(
            NoteEntity(
                id = "n1", title = "Local", content = "vieja",
                updatedAt = 100L, syncStatus = SyncStatus.SYNCED,
            )
        )
        val api = FakeApi(remote = listOf(NoteDto("n1", "Remote", "nueva", 200L)))
        val repo = OfflineFirstNoteRepository(dao, api, NoOpScheduler())

        repo.refreshFromServer()

        val stored = dao.findById("n1")!!
        assertEquals("Remote", stored.title)
        assertEquals(200L, stored.updatedAt)
        assertEquals(SyncStatus.CONFLICT, stored.syncStatus)
    }

    @Test
    fun `local with newer timestamp is preserved`() = runTest {
        val dao = FakeNoteDao()
        dao.upsert(
            NoteEntity(
                id = "n1", title = "Local", content = "reciente",
                updatedAt = 500L, syncStatus = SyncStatus.PENDING,
            )
        )
        val api = FakeApi(remote = listOf(NoteDto("n1", "Remote", "vieja", 100L)))
        val repo = OfflineFirstNoteRepository(dao, api, NoOpScheduler())

        repo.refreshFromServer()

        val stored = dao.findById("n1")!!
        assertEquals("Local", stored.title)
        assertEquals(500L, stored.updatedAt)
        assertEquals(SyncStatus.PENDING, stored.syncStatus)
    }

    @Test
    fun `remote-only note is inserted as SYNCED`() = runTest {
        val dao = FakeNoteDao()
        val api = FakeApi(remote = listOf(NoteDto("nuevo", "T", "C", 999L)))
        val repo = OfflineFirstNoteRepository(dao, api, NoOpScheduler())

        repo.refreshFromServer()

        val stored = dao.findById("nuevo")!!
        assertEquals(SyncStatus.SYNCED, stored.syncStatus)
        assertNull(dao.findById("otro"))
    }

    // -------- Fakes --------

    private class FakeApi(private val remote: List<NoteDto>) : NoteApiService {
        override suspend fun getAllNotes(): List<NoteDto> = remote
        override suspend fun upsertNote(note: NoteDto) = Unit
        override suspend fun deleteNote(id: String) = Unit
    }

    private class FakeNoteDao : NoteDao {
        private val state = MutableStateFlow<Map<String, NoteEntity>>(emptyMap())

        override fun observeAll(): Flow<List<NoteEntity>> = error("not used")
        override suspend fun findById(id: String): NoteEntity? = state.value[id]
        override suspend fun getPending(): List<NoteEntity> =
            state.value.values.filter { it.syncStatus == SyncStatus.PENDING }

        override suspend fun upsert(note: NoteEntity) {
            state.update { it + (note.id to note) }
        }

        override suspend fun markSynced(id: String) {
            state.update { current ->
                val existing = current[id] ?: return@update current
                current + (id to existing.copy(syncStatus = SyncStatus.SYNCED))
            }
        }

        override suspend fun updateStatus(id: String, status: SyncStatus) {
            state.update { current ->
                val existing = current[id] ?: return@update current
                current + (id to existing.copy(syncStatus = status))
            }
        }

        override suspend fun deleteById(id: String) {
            state.update { it - id }
        }
    }

    private class NoOpScheduler : SyncTrigger {
        override fun schedule() { /* sin efecto en tests */ }
    }
}
