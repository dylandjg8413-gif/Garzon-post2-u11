package com.example.app.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.app.domain.model.SyncStatus

/**
 * Representacion fisica de una nota en Room.
 *
 *  - updatedAt: timestamp (millis) de la ultima modificacion. Eje del
 *    algoritmo Last-Write-Wins: gana el registro con mayor updatedAt.
 *  - syncStatus: el Worker filtra por PENDING para saber que subir.
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
)
