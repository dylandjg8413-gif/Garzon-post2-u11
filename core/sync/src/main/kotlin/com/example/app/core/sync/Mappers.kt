package com.example.app.core.sync

import com.example.app.core.database.NoteEntity
import com.example.app.core.network.NoteDto
import com.example.app.domain.model.SyncStatus

/**
 * Mapeos entre las representaciones de transporte (DTO) y persistencia
 * (Entity). Mantener estas conversiones aisladas evita que el dominio
 * conozca formatos concretos del servidor o de Room.
 */

internal fun NoteEntity.toDto(): NoteDto = NoteDto(
    id = id,
    title = title,
    content = content,
    updatedAt = updatedAt,
)

internal fun NoteDto.toEntity(status: SyncStatus = SyncStatus.SYNCED): NoteEntity = NoteEntity(
    id = id,
    title = title,
    content = content,
    updatedAt = updatedAt,
    syncStatus = status,
)
