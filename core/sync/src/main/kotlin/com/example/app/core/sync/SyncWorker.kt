package com.example.app.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.app.core.database.NoteDao
import com.example.app.core.network.NoteApiService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Worker que sube al servidor todas las notas marcadas como PENDING.
 *
 * Comportamiento:
 *  1. Lee las notas PENDING desde Room.
 *  2. Por cada una, llama upsertNote() del backend.
 *  3. Si el upload exitoso, marca la nota SYNCED en local.
 *  4. Ante IOException reintenta con backoff exponencial hasta 3 veces.
 *
 * Instrumentacion:
 *  - Crea un span "notes.sync" con los atributos sync.attempt y
 *    sync.pending_count.
 *  - Captura el resultado final como atributo sync.outcome
 *    (success | retry | failure) y status del span.
 *  - asContextElement() propaga el span a todas las coroutines hijas
 *    para que cualquier llamada anidada pueda anadir spans hijos.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val noteDao: NoteDao,
    private val noteApiService: NoteApiService,
) : CoroutineWorker(context, params) {

    private val tracer = GlobalOpenTelemetry.getTracer(TRACER_NAME)

    override suspend fun doWork(): Result {
        val span = tracer.spanBuilder(SPAN_NAME)
            .setAttribute("sync.attempt", runAttemptCount.toLong())
            .startSpan()

        return withContext(Dispatchers.IO + span.asContextElement()) {
            try {
                val pending = noteDao.getPending()
                span.setAttribute("sync.pending_count", pending.size.toLong())

                pending.forEach { note ->
                    noteApiService.upsertNote(note.toDto())
                    noteDao.markSynced(note.id)
                }

                span.setAttribute("sync.outcome", "success")
                span.setStatus(StatusCode.OK)
                Result.success()
            } catch (e: IOException) {
                span.recordException(e)
                span.setStatus(StatusCode.ERROR, e.message ?: "sync failed")
                val retry = runAttemptCount < MAX_ATTEMPTS
                span.setAttribute("sync.outcome", if (retry) "retry" else "failure")
                if (retry) Result.retry() else Result.failure()
            } finally {
                span.end()
            }
        }
    }

    companion object {
        const val TRACER_NAME = "com.example.app.sync"
        const val SPAN_NAME = "notes.sync"
        const val MAX_ATTEMPTS = 3
    }
}
