package com.example.app.core.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Modulo de Hilt que provee la instancia de Room. La base de datos
 * se construye una sola vez (Singleton) y se inyecta donde haga falta.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "notes.db",
        ).build()
    }

    @Provides
    fun provideNoteDao(database: AppDatabase): NoteDao = database.noteDao()
}
