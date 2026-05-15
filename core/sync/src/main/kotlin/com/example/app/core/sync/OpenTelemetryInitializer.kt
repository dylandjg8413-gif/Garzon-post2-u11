package com.example.app.core.sync

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.exporter.logging.LoggingSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor

/**
 * Inicializa el SDK de OpenTelemetry una sola vez al arranque de la app.
 *
 * Para el laboratorio se usa LoggingSpanExporter: cada span termina
 * imprimiendose en Logcat con el tag
 * "io.opentelemetry.exporter.logging.LoggingSpanExporter".
 * Esto permite verificar los atributos (sync.attempt, sync.pending_count,
 * sync.outcome) sin necesidad de un backend remoto tipo Jaeger.
 *
 * Si en el futuro se quiere enviar a un backend real, basta con anadir
 * un OtlpGrpcSpanExporter al SdkTracerProvider en lugar -- o ademas
 * de -- LoggingSpanExporter.
 */
object OpenTelemetryInitializer {

    private val SERVICE_NAME_KEY: AttributeKey<String> =
        AttributeKey.stringKey("service.name")

    @Volatile
    private var initialized = false

    fun initialize(serviceName: String = "notes-app") {
        if (initialized) return
        synchronized(this) {
            if (initialized) return

            val resource = Resource.getDefault().merge(
                Resource.create(Attributes.of(SERVICE_NAME_KEY, serviceName))
            )

            val tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(LoggingSpanExporter.create()))
                .setResource(resource)
                .build()

            val sdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build()

            GlobalOpenTelemetry.set(sdk)
            initialized = true
        }
    }
}
