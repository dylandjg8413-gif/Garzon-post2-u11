package com.example.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.app.core.sync.OpenTelemetryInitializer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Aplicacion raiz.
 *
 *  - @HiltAndroidApp inicializa el grafo de Hilt.
 *  - Implementa Configuration.Provider para que WorkManager use el
 *    HiltWorkerFactory inyectado; sin esto, SyncWorker no recibiria
 *    NoteDao ni NoteApiService.
 *  - Inicializa OpenTelemetry una sola vez al arranque para que el
 *    Worker pueda obtener el tracer global.
 */
@HiltAndroidApp
class NotesApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        OpenTelemetryInitializer.initialize(serviceName = "notes-app")
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
