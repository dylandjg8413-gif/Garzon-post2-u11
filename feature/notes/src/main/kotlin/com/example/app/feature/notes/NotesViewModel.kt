package com.example.app.feature.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.domain.model.Note
import com.example.app.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel del feature de notas con sincronizacion offline-first.
 *
 *  - uiState (StateFlow): replayable, sobrevive a rotaciones.
 *  - events  (SharedFlow): one-shot, no se reemiten al volver a la pantalla.
 *  - refresh(): dispara la conciliacion LWW desde el servidor. Util cuando
 *               el usuario hace "pull to refresh" o al abrir la app.
 */
@HiltViewModel
class NotesViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotesUiState>(NotesUiState.Loading)
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<NotesEvent>()
    val events: SharedFlow<NotesEvent> = _events.asSharedFlow()

    init {
        observeNotes()
        refresh()
    }

    private fun observeNotes() {
        viewModelScope.launch {
            noteRepository.getNotes()
                .catch { e ->
                    _uiState.value = NotesUiState.Error(e.message ?: "Error desconocido")
                }
                .collect { notes ->
                    _uiState.value = NotesUiState.Success(notes)
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                noteRepository.refreshFromServer()
                _events.emit(NotesEvent.RefreshCompleted)
            } catch (e: Exception) {
                // Sin red: el caching local sigue mostrandose. No se cambia
                // el uiState a Error porque el usuario aun ve datos validos.
                _events.emit(NotesEvent.RefreshFailed(e.message ?: "Sin red"))
            }
        }
    }

    fun onNoteClicked(noteId: String) {
        viewModelScope.launch {
            _events.emit(NotesEvent.NavigateToDetail(noteId))
        }
    }

    fun addNote(title: String, content: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            noteRepository.addNote(
                Note(
                    id = UUID.randomUUID().toString(),
                    title = title.trim(),
                    content = content.trim(),
                )
            )
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            noteRepository.deleteNote(noteId)
            _events.emit(NotesEvent.NoteDeleted)
        }
    }
}
