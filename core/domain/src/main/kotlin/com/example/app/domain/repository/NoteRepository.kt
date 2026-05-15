package com.example.app.domain.repository

import com.example.app.domain.model.Note
import kotlinx.coroutines.flow.Flow

/**
 * Contrato del repositorio de notas en una arquitectura offline-first.
 *
 * Reglas:
 *  - getNotes() expone SIEMPRE lo que hay en el cache local (Room).
 *    Asi la UI funciona sin red.
 *  - addNote/deleteNote escriben primero en local y luego encolan un
 *    trabajo de WorkManager para subir el cambio al servidor.
 *  - refresh() es opcional: descarga lo ultimo del servidor y aplica
 *    LWW para resolver conflictos contra el cache local.
 */
interface NoteRepository {
    fun getNotes(): Flow<List<Note>>
    suspend fun addNote(note: Note)
    suspend fun deleteNote(id: String)

    /**
     * Trae el estado del servidor y mezcla con la copia local aplicando
     * Last-Write-Wins (gana el mayor updatedAt).
     */
    suspend fun refreshFromServer()
}
