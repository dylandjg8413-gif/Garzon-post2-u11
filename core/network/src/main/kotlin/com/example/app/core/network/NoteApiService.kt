package com.example.app.core.network

import java.io.IOException

/**
 * Contrato del cliente HTTP del backend de notas.
 *
 * Se mantiene como interfaz para que el repositorio:
 *  - dependa de una abstraccion (testeable con fakes).
 *  - pueda intercambiar la implementacion (MockWebServer en dev,
 *    Retrofit en produccion) sin tocar la capa de aplicacion.
 */
interface NoteApiService {

    /** Devuelve todas las notas del servidor. Puede lanzar IOException. */
    @Throws(IOException::class)
    suspend fun getAllNotes(): List<NoteDto>

    /** Crea o actualiza la nota en el servidor (idempotente por id). */
    @Throws(IOException::class)
    suspend fun upsertNote(note: NoteDto)

    /** Elimina la nota del servidor. */
    @Throws(IOException::class)
    suspend fun deleteNote(id: String)
}
