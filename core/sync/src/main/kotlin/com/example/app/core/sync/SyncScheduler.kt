package com.example.app.core.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementacion del SyncTrigger basada en WorkManager.
 *
 * - enqueueUniqueWork con KEEP: si ya hay un sync pendiente no se duplica;
 *   el trabajo en curso (o ya encolado) se conserva.
 * - Constraint NetworkType.CONNECTED: WorkManager espera tener red para
 *   ejecutar. Esto materializa el "offline-first": los cambios se guardan
 *   ya en Room y el sync ocurre cuando puede.
 * - BackoffPolicy.EXPONENTIAL con 15 s iniciales: 15s -> 30s -> 60s...
 *   ante IOException reportados por Result.retry().
 */
@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : SyncTrigger {

    override fun schedule() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                INITIAL_BACKOFF_SECONDS,
                TimeUnit.SECONDS,
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    companion object {
        const val UNIQUE_WORK_NAME = "notes-sync"
        const val INITIAL_BACKOFF_SECONDS = 15L
    }
}

/**
 * Modulo Hilt que expone SyncScheduler como la implementacion del trigger.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {
    @Binds
    @Singleton
    abstract fun bindSyncTrigger(impl: SyncScheduler): SyncTrigger
}
