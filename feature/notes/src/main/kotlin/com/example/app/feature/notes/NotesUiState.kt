package com.example.app.feature.notes

import com.example.app.domain.model.Note

/**
 * Estado de la pantalla de notas. Se modela como sealed class para forzar
 * exhaustividad en el when del Composable y evitar estados invalidos
 * (por ejemplo, "cargando con error" o "exito sin lista").
 */
sealed class NotesUiState {
    data object Loading : NotesUiState()
    data class Success(val notes: List<Note>) : NotesUiState()
    data class Error(val message: String) : NotesUiState()
}
