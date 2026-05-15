package com.example.app.feature.notes

import com.example.app.domain.model.Note
import com.example.app.domain.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Verifica el comportamiento reactivo del ViewModel contra un repo fake.
 * Casos cubiertos:
 *   - uiState emite Success cuando el repo entrega notas.
 *   - addNote propaga al StateFlow.
 *   - deleteNote emite NoteDeleted en el SharedFlow.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: FakeNoteRepository
    private lateinit var viewModel: NotesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repo = FakeNoteRepository()
        viewModel = NotesViewModel(repo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState emits Success when repository emits notes`() = runTest(dispatcher) {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue("Estado debe ser Success", state is NotesUiState.Success)
        assertEquals(1, (state as NotesUiState.Success).notes.size)
    }

    @Test
    fun `addNote propagates to uiState`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.addNote(title = "Nueva", content = "Cuerpo")
        advanceUntilIdle()

        val notes = (viewModel.uiState.value as NotesUiState.Success).notes
        assertEquals(2, notes.size)
        assertTrue(notes.any { it.title == "Nueva" })
    }

    @Test
    fun `deleteNote emits NoteDeleted event`() = runTest(dispatcher) {
        advanceUntilIdle()
        val firstId = (viewModel.uiState.value as NotesUiState.Success).notes.first().id

        val eventDeferred = async { viewModel.events.first { it is NotesEvent.NoteDeleted } }
        advanceUntilIdle()

        viewModel.deleteNote(firstId)
        advanceUntilIdle()

        assertEquals(NotesEvent.NoteDeleted, eventDeferred.await())
        assertTrue((viewModel.uiState.value as NotesUiState.Success).notes.none { it.id == firstId })
    }

    private class FakeNoteRepository : NoteRepository {
        private val state = MutableStateFlow(
            listOf(Note(id = "seed", title = "Inicial", content = "Seed"))
        )

        override fun getNotes(): Flow<List<Note>> = state.asStateFlow()

        override suspend fun addNote(note: Note) {
            state.update { it + note }
        }

        override suspend fun deleteNote(id: String) {
            state.update { it.filterNot { n -> n.id == id } }
        }

        override suspend fun refreshFromServer() {
            // no-op para los tests del ViewModel
        }
    }
}
