# Capturas de evidencia - U11 Post 2

Esta carpeta debe contener las capturas referenciadas desde el `README.md`
raiz. Mantener exactamente los nombres siguientes:

| Archivo                                | Que debe mostrar |
|----------------------------------------|------------------|
| `checkpoint1-room-schema.png`          | Database Inspector mostrando la tabla `notes` con las columnas `id`, `title`, `content`, `updatedAt`, `syncStatus`. |
| `checkpoint2-sync-workmanager.png`     | Background Task Inspector con el job `notes-sync` en estado `SUCCEEDED`. Opcional: lista de notas con badges pasando de PENDING a SYNCED. |
| `checkpoint3-lww-conflict.png`         | Pantalla de la app o Database Inspector mostrando una nota con `syncStatus = CONFLICT` tras el primer refresh. |
| `checkpoint4-otel-logcat.png`          | Logcat filtrado por `LoggingSpanExporter` mostrando el span `notes.sync` con los atributos `sync.attempt`, `sync.pending_count` y `sync.outcome`. |
