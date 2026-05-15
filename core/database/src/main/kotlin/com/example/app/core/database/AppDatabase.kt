package com.example.app.core.database

import androidx.room.Database
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.RoomDatabase
import com.example.app.domain.model.SyncStatus

/**
 * Database raiz. Version 1 nace ya con el campo syncStatus, por lo que NO
 * se necesita una migracion declarada. Si en el futuro se agregan campos
 * (por ejemplo deletedAt para soft-delete), se incrementa la version y
 * se declara una migracion en este archivo.
 */
@Database(
    entities = [NoteEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(SyncStatusConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}

/**
 * Room no sabe persistir enums por defecto: el converter mapea el enum
 * a su nombre (String) para que el filtro `WHERE syncStatus = 'PENDING'`
 * funcione en SQL plano.
 */
class SyncStatusConverter {
    @TypeConverter
    fun fromStatus(status: SyncStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): SyncStatus = SyncStatus.valueOf(value)
}
