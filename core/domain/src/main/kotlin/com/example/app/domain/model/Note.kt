package com.example.app.domain.model

/**
 * Entidad de dominio que representa una nota.
 *
 * Incluye:
 *  - updatedAt: timestamp de la ultima modificacion. Es el campo clave
 *    para resolver conflictos con Last-Write-Wins.
 *  - syncStatus: estado de sincronizacion con el servidor. La UI lo
 *    refleja con un indicador (icono o badge).
 */
data class Note(
    val id: String,
    val title: String,
    val content: String,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
)

/**
 * Estado de sincronizacion de una nota con el backend.
 *
 *  - PENDING:  modificada localmente, aun no enviada al servidor.
 *  - SYNCED:   confirmada por el servidor; en paridad con el backend.
 *  - CONFLICT: el servidor tenia una version mas nueva que la local
 *              en el ultimo refresh; LWW eligio la del servidor.
 */
enum class SyncStatus { PENDING, SYNCED, CONFLICT }
