# Unidad 11 - Post Contenido 2: Sincronizacion Offline-First con Observabilidad

App Android multi-modulo que extiende el feature de notas (post 1) con:

- **Persistencia local** con Room (`:core:database`) y campo `syncStatus`.
- **Sincronizacion offline-first** disparada por **WorkManager** con
  retry exponencial (`:core:sync`).
- **Resolucion de conflictos Last-Write-Wins (LWW)** en
  `OfflineFirstNoteRepository`.
- **Trazas OpenTelemetry** que registran cada ejecucion del sync con
  atributos de diagnostico (`sync.attempt`, `sync.pending_count`,
  `sync.outcome`).

---

## Diagrama del flujo offline-first

```
                +--------------+
   Usuario ---> | NotesScreen  |
                +-----+--------+
                      | addNote / deleteNote
                      v
            +-----------------------+
            | NotesViewModel        |   (StateFlow + SharedFlow del post 1)
            +-----------+-----------+
                        |
                        v
        +---------------------------------+
        | OfflineFirstNoteRepository      |
        |                                 |
        |  1. upsert Room (PENDING)       |
        |  2. syncTrigger.schedule()      |
        +-----+------------------+--------+
              |                  |
              v                  v
       +-------------+    +-------------------+
       |  Room DAO   |    | SyncScheduler     |
       | (notes.db)  |    | WorkManager       |
       +------+------+    +---------+---------+
              ^                     |
              |                     v
              |             +----------------+
              |             | SyncWorker     |
              |             | (instrumentado |
              |             |  con OTel)     |
              |             +----+-----------+
              |                  |
              |    markSynced    | upsertNote(DTO)
              +------------------+----+
                                      v
                              +-------------+
                              | NoteApi     |
                              | (mock /     |
                              |  Retrofit)  |
                              +-------------+

    Logcat <-- LoggingSpanExporter <-- OpenTelemetry SDK
```

### Flujo end-to-end (escritura offline)

1. Usuario crea una nota sin red.
2. `OfflineFirstNoteRepository.addNote` la guarda en Room con
   `syncStatus = PENDING` y llama `syncTrigger.schedule()`.
3. WorkManager encola `SyncWorker` con la constraint
   `NetworkType.CONNECTED` -- queda esperando red.
4. El usuario activa WiFi. WorkManager dispara el Worker.
5. `SyncWorker` abre el span `notes.sync` (atributo
   `sync.attempt = 0`), lee las PENDING (`sync.pending_count = N`) y
   las sube con `NoteApi.upsertNote`. Si una llamada falla con
   `IOException`, marca `sync.outcome = retry` y devuelve `Result.retry()`;
   WorkManager reintenta con backoff exponencial (15s, 30s, 60s).
6. Cuando una nota se sube exitosamente, `NoteDao.markSynced(id)`
   actualiza `syncStatus = SYNCED`. La UI se actualiza reactivamente
   porque `getNotes()` expone un `Flow`.

### Algoritmo Last-Write-Wins (LWW)

`OfflineFirstNoteRepository.refreshFromServer()` recorre las notas del
servidor y compara timestamps:

| Caso                                              | Accion |
|---------------------------------------------------|--------|
| Nota no existe localmente                          | Insertar como `SYNCED`. |
| Remoto `updatedAt` > Local `updatedAt`             | Sobrescribir; marcar `CONFLICT` (otro dispositivo edito). |
| Remoto `updatedAt` <= Local `updatedAt`            | No tocar nada; el Worker subira la version local en el proximo ciclo. |

La precondicion para que LWW sea correcto es que todos los clientes
sincronicen el reloj con NTP; de lo contrario hay riesgo de inversion.

---

## Estructura de modulos

```
NotesAppSync
├── app                          # Configuracion + WorkManagerFactory + OTel init
│   ├── data/OfflineFirstNoteRepository.kt   <-- aplica LWW
│   ├── di/RepositoryModule.kt
│   └── MainActivity.kt
│
├── core
│   ├── domain          (JVM puro)   Note + SyncStatus + NoteRepository
│   ├── ui              (Android lib) Theme Material3
│   ├── database        (Android lib) Room: NoteEntity, NoteDao, AppDatabase
│   ├── network         (Android lib) NoteApiService + FakeNoteApiService
│   └── sync            (Android lib) SyncWorker, SyncScheduler, SyncTrigger,
│                                     OpenTelemetryInitializer
│
└── feature
    └── notes           NotesScreen muestra badge PENDING/SYNCED/CONFLICT
                        y TopAppBar con accion "Sync" -> refreshFromServer().
```

### Dependencias entre modulos

```
:app -----> :feature:notes -----> :core:domain
   \              \---------> :core:ui
    \---> :core:database  ---> :core:domain
    \---> :core:network   ---> :core:domain
    \---> :core:sync      ---> :core:database
                          \--> :core:network
```

---

## Decisiones de diseno

### 1. `SyncTrigger` como abstraccion de WorkManager

`OfflineFirstNoteRepository` recibe un `SyncTrigger` (interfaz), no
`SyncScheduler` (impl concreta). Esto permite **testear el repositorio
sin WorkManager** (que requiere `Context`). En produccion Hilt enlaza
`SyncTrigger` -> `SyncScheduler` mediante `SyncModule`.

### 2. LWW se aplica al `refresh`, no al worker

El Worker SOLO sube cambios locales. La conciliacion contra el
servidor (que es donde aparecen los conflictos) se hace en
`refreshFromServer()`, disparado por la UI con el boton "Sync" o por
el `init` del ViewModel. Separar ambos roles mantiene el Worker
simple y predecible.

### 3. `SyncStatus = CONFLICT` como senal a la UI

Cuando el servidor sobrescribe una nota local (porque tenia
`updatedAt` mas reciente), la nota queda marcada `CONFLICT`. La UI
muestra un badge rojo para que el usuario sepa que sus cambios
locales se perdieron. Esto NO es "resolucion de conflictos
automatica" pura -- es LWW con visibilidad.

### 4. `LoggingSpanExporter` en vez de un backend OTel real

Para que el laboratorio sea reproducible sin desplegar Jaeger o
Honeycomb, los spans se exportan a Logcat con
`LoggingSpanExporter`. Para conmutar a un backend OTLP basta con
sustituir el exporter en `OpenTelemetryInitializer`.

### 5. WorkManager con `Configuration.Provider` + HiltWorkerFactory

`NotesApplication` implementa `Configuration.Provider` y se desactiva
el `WorkManagerInitializer` por defecto en el manifest. Sin esto,
`SyncWorker` no podria recibir `NoteDao` ni `NoteApiService` por
constructor.

---

## Como ejecutar

```powershell
# Compilar todos los modulos
./gradlew assembleDebug

# Tests unitarios
./gradlew test

# Tests especificos
./gradlew :app:test
./gradlew :feature:notes:test
```

Abrir en Android Studio Hedgehog+ y ejecutar `:app` en un emulador con
API 24+ con conexion de red disponible.

---

## Checkpoints / Evidencias

### Checkpoint 1 - Esquema Room con `syncStatus`

Tras instalar la app, abrir **App Inspection > Database Inspector**.
Tabla `notes` debe tener las columnas: `id`, `title`, `content`,
`updatedAt`, `syncStatus`.

![Checkpoint 1 - Room schema](screenshots/checkpoint1-room-schema.png)

### Checkpoint 2 - Sync exitoso al recuperar red

1. Activar modo avion en el emulador.
2. Crear 2-3 notas (aparecen con badge **PENDING**).
3. Desactivar modo avion.
4. WorkManager dispara `SyncWorker`. En **App Inspection > Background Task Inspector**
   el job `notes-sync` aparece en estado `SUCCEEDED`.
5. Las notas pasan a badge **SYNCED** en la lista.

![Checkpoint 2 - Sync workmanager](screenshots/checkpoint2-sync-workmanager.png)

### Checkpoint 3 - Resolucion LWW

El backend mock incluye una nota `remote-seed` con `updatedAt` en el
futuro. Al primer `refresh`, esa nota aparece marcada **CONFLICT**
(LWW: el servidor gano). Capturar la pantalla y/o el Database
Inspector mostrando `syncStatus = 'CONFLICT'` para ese registro.

![Checkpoint 3 - LWW](screenshots/checkpoint3-lww-conflict.png)

### Checkpoint 4 - Spans de OpenTelemetry en Logcat

Filtrar Logcat por el tag `LoggingSpanExporter`. Cada ejecucion del
`SyncWorker` produce una linea similar a:

```
'notes.sync' : <traceId> <spanId> INTERNAL [...]
AttributesMap{data={
   service.name=notes-app,
   sync.attempt=0,
   sync.pending_count=3,
   sync.outcome=success
}, capacity=128, totalAddedValues=4}
```

![Checkpoint 4 - OTel logcat](screenshots/checkpoint4-otel-logcat.png)

---

## Stack tecnico

- Kotlin 1.9.24 + Coroutines 1.8.1
- Gradle 8.x con Version Catalog (`gradle/libs.versions.toml`)
- AGP 8.5.2 / compileSdk 34 / minSdk 24
- Jetpack Compose (BoM 2024.06.00) + Material 3
- Hilt 2.51.1 + Hilt-Work 1.2.0
- **Room 2.6.1** (cache local + Flow reactivo)
- **WorkManager 2.9.0** (sync diferido con constraints + backoff)
- **OpenTelemetry 1.38.0** (api + sdk + exporter-logging + extension-kotlin)
- OkHttp 4.12.0 + MockWebServer 4.12.0 (preparados para un backend HTTP real)
