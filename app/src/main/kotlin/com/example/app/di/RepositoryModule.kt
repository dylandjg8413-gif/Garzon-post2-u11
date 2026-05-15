package com.example.app.di

import com.example.app.data.OfflineFirstNoteRepository
import com.example.app.domain.repository.NoteRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Mapea el contrato del dominio a la implementacion offline-first que
 * combina Room (cache local), NoteApiService (backend) y SyncScheduler
 * (WorkManager). El feature de notas pide NoteRepository y Hilt entrega
 * esta implementacion concreta sin que la capa de presentacion lo sepa.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindNoteRepository(
        impl: OfflineFirstNoteRepository,
    ): NoteRepository
}
