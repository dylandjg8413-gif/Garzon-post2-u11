package com.example.app.core.network

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementacion fake del backend.
 *
 * Comportamiento deliberadamente "molesto" para poder demostrar los
 * casos interesantes del laboratorio:
 *
 *  - Latencia simulada (~400 ms por llamada).
 *  - Falla las primeras 'failuresBeforeSuccess' llamadas con IOException
 *    para evidenciar el retry con backoff exponencial del Worker.
 *  - Mantiene su propia copia de las notas; precarga una nota con un
 *    updatedAt en el futuro para forzar un conflicto LWW al primer
 *    refresh.
 */
@Singleton
class FakeNoteApiService @Inject constructor() : NoteApiService {

    private val mutex = Mutex()
    private val attempts = AtomicInteger(0)
    private val failuresBeforeSuccess = 1

    private val store: MutableMap<String, NoteDto> = mutableMapOf(
        "remote-seed" to NoteDto(
            id = "remote-seed",
            title = "Nota editada en otro dispositivo",
            content = "Si el cliente tiene una version mas antigua, LWW la sobrescribe.",
            updatedAt = System.currentTimeMillis() + 60_000L,
        ),
    )

    override suspend fun getAllNotes(): List<NoteDto> = mutex.withLock {
        delay(400)
        store.values.toList()
    }

    override suspend fun upsertNote(note: NoteDto) {
        delay(400)
        // Simula error transitorio en los primeros intentos para que el
        // Worker tenga que reintentar (y los spans muestren sync.attempt).
        val attempt = attempts.incrementAndGet()
        if (attempt <= failuresBeforeSuccess) {
            throw IOException("Backend no disponible (intento $attempt)")
        }
        mutex.withLock { store[note.id] = note }
    }

    override suspend fun deleteNote(id: String) {
        delay(200)
        mutex.withLock { store.remove(id) }
    }
}
