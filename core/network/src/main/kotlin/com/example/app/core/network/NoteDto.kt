package com.example.app.core.network

/**
 * Representacion de la nota tal como viaja por la red. Se mantiene
 * separada de NoteEntity (Room) y de Note (dominio) para evitar acoplar
 * formatos de almacenamiento o presentacion al contrato del backend.
 */
data class NoteDto(
    val id: String,
    val title: String,
    val content: String,
    val updatedAt: Long,
)
