package com.example.app.core.network

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Mapea el contrato del API a su implementacion fake. Cuando se conecte
 * el backend real basta con sustituir FakeNoteApiService por una impl
 * basada en Retrofit; la app no se entera.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    @Binds
    @Singleton
    abstract fun bindNoteApiService(impl: FakeNoteApiService): NoteApiService
}
