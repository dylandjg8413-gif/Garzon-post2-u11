Unidad 11 - Post Contenido 2: Sincronización Reactiva Offline-First con Telemetría

Aplicación Android multi-módulo que amplía el feature de tareas desarrollado en el Post Contenido 1 incorporando persistencia local, sincronización offline-first y monitoreo mediante OpenTelemetry.

La aplicación implementa:

Persistencia local usando Room (:core:storage) con estados de sincronización.
Sincronización automática utilizando WorkManager (:core:sync).
Estrategia de resolución de conflictos Last-Update-Wins (LUW).
Observabilidad mediante OpenTelemetry y LoggingSpanExporter.
Arquitectura desacoplada y reactiva basada en Kotlin Flow.
Flujo offline-first de sincronización
                +----------------+
 Usuario -----> | TaskScreen     |
                +-------+--------+
                        |
                        | createTask / deleteTask
                        v
            +-------------------------+
            | TaskViewModel           |
            | StateFlow + SharedFlow  |
            +------------+------------+
                         |
                         v
        +--------------------------------------+
        | OfflineTaskRepository                |
        |                                      |
        | 1. guarda en Room (PENDING)          |
        | 2. programa sincronizacion           |
        +--------------+-----------------------+
                       |
          +------------+------------+
          |                         |
          v                         v
   +-------------+          +------------------+
   | Room DAO    |          | SyncManager      |
   | tasks.db    |          | WorkManager      |
   +------+------+          +---------+--------+
          ^                            |
          |                            v
          |                   +----------------+
          |                   | SyncWorker     |
          |                   | OpenTelemetry  |
          |                   +-------+--------+
          |                           |
          +---------------------------+
                                      |
                                      v
                             +----------------+
                             | Remote API     |
                             | FakeTaskApi    |
                             +----------------+
Flujo completo offline-first
1. Usuario crea tareas sin conexión

La aplicación:

Guarda localmente en Room.
Marca las tareas como PENDING.
Programa sincronización diferida.
2. WorkManager espera conectividad

El SyncWorker se ejecuta únicamente cuando:

NetworkType.CONNECTED

está disponible.

3. Recuperación automática de red

Cuando vuelve internet:

WorkManager ejecuta SyncWorker.
Las tareas pendientes se sincronizan.
Room actualiza el estado a SYNCED.
La UI se actualiza automáticamente mediante Flow.
4. Manejo de errores y retry exponencial

Si ocurre un error de red:

Result.retry()

WorkManager aplica backoff exponencial:

15s -> 30s -> 60s -> 120s
Algoritmo Last-Update-Wins (LUW)

OfflineTaskRepository.refreshFromServer() compara timestamps locales y remotos.

Caso	Acción
No existe localmente	Insertar
Remoto más reciente	Sobrescribir
Local más reciente	Mantener local
Ejemplo de comparación
if(remote.updatedAt > local.updatedAt) {
    overwriteLocal()
} else {
    keepLocalVersion()
}
Estados de sincronización
Estado	Significado
PENDING	Cambios pendientes
SYNCED	Sincronizado correctamente
CONFLICT	Conflicto detectado
Estructura multi-módulo
TaskSyncApp
├── app
│   ├── data/OfflineTaskRepository.kt
│   ├── di/AppModule.kt
│   ├── MainActivity.kt
│   └── TaskSyncApplication.kt
│
├── core
│   ├── domain
│   │   ├── model/Task.kt
│   │   ├── model/SyncStatus.kt
│   │   └── repository/TaskRepository.kt
│   │
│   ├── ui
│   │   └── theme/AppTheme.kt
│   │
│   ├── storage
│   │   ├── TaskDao.kt
│   │   ├── TaskEntity.kt
│   │   └── AppDatabase.kt
│   │
│   ├── network
│   │   ├── TaskApiService.kt
│   │   └── FakeTaskApiService.kt
│   │
│   └── sync
│       ├── SyncWorker.kt
│       ├── SyncScheduler.kt
│       ├── SyncTrigger.kt
│       └── TelemetryInitializer.kt
│
└── feature
    └── tasks
        ├── TaskScreen.kt
        ├── TaskViewModel.kt
        ├── TaskUiState.kt
        ├── TaskEvent.kt
        └── components/
Dependencias entre módulos
:app -----------> :feature:tasks -----------> :core:domain
   \                       \---------------> :core:ui
    \
     +------> :core:storage -------------> :core:domain
     +------> :core:network -------------> :core:domain
     +------> :core:sync
Decisiones de diseño
1. Abstracción SyncTrigger

El repositorio depende de:

interface SyncTrigger

y NO directamente de WorkManager.

Beneficios:

Fácil testing.
Bajo acoplamiento.
Independencia de Android Framework.
2. Separación entre sincronización y conciliación

El SyncWorker:

solo sube cambios locales.

La reconciliación:

ocurre en refreshFromServer().

Esto mantiene el Worker simple y determinista.

3. Estado CONFLICT visible en UI

Cuando el servidor sobrescribe cambios locales:

syncStatus = CONFLICT

La interfaz muestra:

Badge rojo.
Advertencia visual.
Estado reactivo inmediato.
4. OpenTelemetry con LoggingSpanExporter

En lugar de usar Jaeger o Grafana:

LoggingSpanExporter.create()

exporta spans directamente a Logcat.

Ventajas:

Laboratorio reproducible.
Sin infraestructura externa.
Fácil depuración local.
5. WorkManager + Hilt

TaskSyncApplication implementa:

Configuration.Provider

permitiendo inyección en:

SyncWorker
Ejecución del proyecto
Compilar todos los módulos
./gradlew assembleDebug
Ejecutar pruebas
./gradlew test
Ejecutar pruebas específicas
./gradlew :app:test
./gradlew :feature:tasks:test
Checkpoints / Evidencias
Checkpoint 1 - Room configurado

La tabla tasks contiene:

id
title
description
updatedAt
syncStatus
Evidencia
Checkpoint 1 - Room schema
Checkpoint 2 - Sincronización automática

Pasos:

Activar modo avión.
Crear tareas.
Aparecen como PENDING.
Restaurar internet.
WorkManager sincroniza automáticamente.
Estados cambian a SYNCED.
Evidencia
Checkpoint 2 - Sync Worker SUCCESS
Checkpoint 3 - Resolución de conflictos

El backend mock contiene datos con timestamps más recientes.

Resultado:

syncStatus = CONFLICT
Evidencia
Checkpoint 3 - conflicto detectado
Checkpoint 4 - OpenTelemetry en Logcat

Filtrar Logcat por:

LoggingSpanExporter

Salida esperada:

'tasks.sync'
AttributesMap{
 sync.attempt=0,
 sync.pending_count=3,
 sync.outcome=success
}
Evidencia
Checkpoint 4 - trazas OpenTelemetry
Stack tecnológico
Tecnología	Versión
Kotlin	1.9.24
Coroutines	1.8.1
AGP	8.5.2
Gradle	8.x
compileSdk	34
minSdk	24
Jetpack Compose	BOM 2024.06.00
Material 3	Última estable
Hilt	2.51.1
Hilt Work	1.2.0
Room	2.6.1
WorkManager	2.9.0
OpenTelemetry	1.38.0
OkHttp	4.12.0
MockWebServer	4.12.0
