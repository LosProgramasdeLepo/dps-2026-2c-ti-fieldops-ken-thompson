# FieldOps

Plataforma de expediciones científicas y operaciones de campo.

## Build

Requiere JDK 25. El Maven Wrapper baja Maven 3.9.11 la primera vez.

```
./mvnw verify
```

En Windows (cmd o PowerShell): `mvnw.cmd verify`.

Los tests de persistencia levantan PostgreSQL y MongoDB con Testcontainers, así que `verify` necesita Docker corriendo.

## Ejecutar

### Backend

El perfil de Spring elige dónde guarda el backend: `sql` (PostgreSQL, por defecto), `mongo` (MongoDB) o `memory` (en memoria, sin base). `compose.yaml` levanta PostgreSQL (base, usuario y contraseña `fieldops`, puerto 5432) y MongoDB como replica set `rs0` (puerto 27017):

```
docker compose up -d --wait
./mvnw package -DskipTests
java -jar frameworks/target/fieldops-frameworks-1.0-SNAPSHOT.jar
```

`--wait` espera a que MongoDB termine de iniciar el replica set (`rs0`); sin eso el perfil `mongo` falla al arrancar.

Con `sql`, Flyway crea o actualiza el esquema al arrancar. La API queda en `http://localhost:8080/v1`. Otro puerto: `--server.port=9090`. Otra base: `FIELDOPS_DB_URL`, `FIELDOPS_DB_USER` y `FIELDOPS_DB_PASSWORD` para PostgreSQL, `FIELDOPS_MONGO_URI` para MongoDB.

Perfiles (se activa uno de almacenamiento y, si se quiere, `demo`):

- `--spring.profiles.active=mongo` guarda en MongoDB.
- `--spring.profiles.active=sql,demo` (o `mongo,demo`) carga los datos de demostración si no hay planes: un relevamiento aprobado con doce actividades de los seis tipos, un bloque compuesto, recursos compartidos, una advertencia aceptada, la corrida iniciada, un incidente cuya propuesta de replanificación aparece después del primer despacho de eventos, y un plan en borrador con errores críticos. Se puede repetir: si ya hay planes, no carga nada.
- `--spring.profiles.active=memory` guarda en memoria y no necesita base; los datos se pierden al cerrar.

Activar solo `demo` deja la aplicación sin almacenamiento: el perfil por defecto (`sql`) se usa únicamente cuando no hay ninguno activo.

Para empezar de cero: `docker compose down -v`.

### Configuración dinámica

Al arrancar, el backend pide propiedades extra al config server de la materia (`dynamic-config-server`, puerto 8888) para la aplicación `fieldops` y sus perfiles. Es opcional: si no está corriendo, arranca con los valores por defecto. Otra URL: `FIELDOPS_CONFIG_SERVER_URL`. Después de cambiar un valor en el server, `POST /actuator/refresh` lo aplica sin reiniciar y devuelve las claves que cambiaron.

| Propiedad | Por defecto | Efecto |
|---|---|---|
| `fieldops.features.assignment-suggestions` | `true` | Feature flag: con `false`, `GET /expeditions/{id}/assignment-suggestions` responde 404 |
| `fieldops.maintenance.enabled` | `false` | Modo mantenimiento: toda la API (`/v1/**`) responde 503 |
| `fieldops.maintenance.message` | (texto) | Detalle del 503 |
| `fieldops.maintenance.retry-after-seconds` | `60` | Encabezado `Retry-After` del 503 |
| `fieldops.notifications.simulate-failure` | `false` | Herramienta de demo: con `true`, el consumidor `notify-responsibles` falla a propósito y sus entregas quedan `RETRYING` y después `FAILED`; `replan-on-incident` sigue funcionando |

Demo: levantar primero `dynamic-config-server` y después el backend; en `http://localhost:8888` agregar una propiedad para la aplicación `fieldops` (perfil `default`), aplicar con `curl -X POST http://localhost:8080/actuator/refresh` y probar la API. `/actuator/health` sigue respondiendo en mantenimiento, así que siempre se puede apagar.

### Despacho de eventos

Un despachador corre dentro del backend, lee el registro de eventos y entrega cada evento a sus consumidores, fuera del request. Estas propiedades se leen al arrancar:

| Propiedad | Por defecto | Efecto |
|---|---|---|
| `fieldops.events.dispatcher.enabled` | `true` | Con `false` no hay disparo periódico: los tests lo apagan y despachan a mano |
| `fieldops.events.dispatch-delay` | `500ms` | Pausa entre pasadas |
| `fieldops.events.batch-size` | `50` | Eventos o entregas por pasada, desde 1 |
| `fieldops.events.max-attempts` | `5` | Intentos antes de `FAILED`, desde 1 |
| `fieldops.events.backoff-base` | `1s` | Espera después del primer fallo; se duplica en cada intento |
| `fieldops.events.backoff-max` | `1m` | Tope de la espera |

Consumidores:

| Consumidor | Eventos | Efecto |
|---|---|---|
| `replan-on-incident` | `IncidentReported` con `activityId` | Crea una propuesta de replanificación, una por incidente |
| `notify-responsibles` | `ExpeditionApproved`, `IncidentReported`, `ReplanProposed` | Deja una notificación por responsable del plan vigente, una por evento y persona |

### Demo de la Entrega 3

El guion, con preparación, lo que muestra cada paso y las preguntas probables, está en [docs/presentacion-entrega-3.md](docs/presentacion-entrega-3.md). Hay que arrancar de cero (`docker compose down -v`) y, para el paso de reintentos, tener el config server en 8888. Las actividades del relevamiento de demo tienen ventana desde mañana: no se inician; se registran incidentes sobre la corrida que ya está abierta.

### Frontend

Requiere Node 22.

```
cd frontend
npm install
npm run dev
```

La aplicación queda en `http://localhost:5173` y redirige `/v1` al backend. La pestaña "Eventos y auditoría" de un plan muestra sus eventos y el estado de cada entrega, y permite reintentar una fallida; en Personas, "Notificaciones" abre la bandeja de cada una. Si el backend corre en otro puerto: `FIELDOPS_API=http://localhost:9090 npm run dev`.

`npm run build` verifica los tipos y genera `frontend/dist`; `npm run preview` la sirve con el mismo redireccionamiento.

Los instantes se cargan y se muestran en UTC.

## Contrato de la API

`http://localhost:8080/v1`, JSON. Instantes y duraciones en ISO-8601 (`PT2H`). Un período es `{start, end}`. Jackson rechaza propiedades que no conoce. Un alta responde 201 con `Location` y `{id}`. Las demás escrituras responden 204. Los errores son `application/problem+json` (RFC 9457).

Paginan, en orden de alta, `GET /expeditions`, `/people`, `/certifications`, `/vehicles`, `/instruments`, `/consumables`, `/permits`, `/expeditions/{id}/replan-proposals`, `/expeditions/{id}/events`, `/events` y `/people/{id}/notifications` (esta última, de la más nueva a la más vieja). `page` desde 0 (por defecto 0), `size` de 1 a 100 (por defecto 20). Cuerpo: `{items, page, size, totalItems, totalPages}`. `Link` (RFC 8288): `first`, `prev` si `page` > 0, `next` si no es la última, `last`. Las sugerencias de asignación y la validación no paginan.

- JSON mal formado, UUID inválido, body que no pasa la validación, o `page`/`size` inválidos: 400
- id del path desconocido: 404. `GET /expeditions/{desconocido}` es 404
- referencia del body desconocida, valor inválido, itinerario o asignación inválidos, u otra regla de dominio: 422. `POST /expeditions` con un responsable inexistente es 422
- transición de plan o de corrida inválida, plan que no se puede aprobar, o reintento de una entrega que no está `FAILED`: 409
- sugerencias de asignación apagadas por configuración: 404
- modo mantenimiento: 503, con `Retry-After`
- cualquier otro error: 500, detalle genérico, sin mensaje interno ni stack trace

### Catálogo

`POST` crea y `GET` lista o lee uno.

- `/certifications`: `{name}`
- `/people`: `{name, certifications[], availability}`. `PUT /people/{id}/certifications/{certificationId}`. `PUT /people/{id}/availability` con `{availability}`
- `/vehicles`: `{capacity, availability}`. `PUT /vehicles/{id}/availability`
- `/instruments`: `{kind, availability}`. `PUT /instruments/{id}/availability`
- `/consumables`: `{name, stock}`. `PUT /consumables/{id}/stock` con `{stock}`
- `/permits`: `{kind, zone, validity}`

`GET /people/{id}/notifications` es la bandeja de la persona: `[{eventId, type, expeditionId, sentAt}]`, una por evento que la alcanzó como responsable del plan vigente (aprobación, incidente o propuesta). La deja el consumidor `notify-responsibles`, así que aparece después del despacho. 404 si la persona no existe.

`availability` es `{periods: [{start, end}]}`. El `kind` de un permiso es `zone` o `night operation`. `capacity` es un entero positivo y `stock` uno no negativo.

### Planes

`POST /expeditions` recibe `{objectives[], period, zones[], responsibles[], restrictions[]}`. `GET /expeditions` devuelve resúmenes (`id`, `version`, `supersedes`, `status`, `charter`). `GET /expeditions/{id}` devuelve charter, itinerario, asignaciones, permisos y advertencias aceptadas. `status`: `DRAFT`, `IN_REVIEW`, `APPROVED`, `SUPERSEDED`.

`POST /expeditions/{id}/activities`. `Location`: `/v1/expeditions/{id}/activities/{activityId}`. `kind`: `SAMPLING`, `MEASUREMENT`, `TRANSIT`, `NIGHT`, `DIVE`, `CAMP`. Comunes: `name`, `estimatedDuration`, `risk` (`LOW`, `MEDIUM`, `HIGH`), `consumption` (id de consumible a cantidad, desde 1), `zone`, `window`, `predecessors[]`. `SAMPLING`, `NIGHT` y `DIVE` agregan `certification`. `MEASUREMENT` agrega `certification` e `instrument`. `TRANSIT` y `CAMP` no agregan campos. `NIGHT` pide además iluminación y permiso nocturno, y el riesgo sube un nivel. El `GET` no devuelve `kind`: devuelve `requirements` (`certifications`, `heldByEveryone`, `instruments`, `specialPermits`, `vehicles`).

`POST /expeditions/{id}/blocks` responde 201 con el bloque y `Location` `/v1/expeditions/{id}`. `node` `BLOCK`: `arrangement` `SEQUENTIAL` o `PARALLEL`, y `parts` (al menos dos). `node` `ACTIVITY`: `activity` con la forma de arriba.

`PUT /expeditions/{id}/activities/{activityId}/predecessors/{predecessorId}` agrega una precedencia. `POST /expeditions/{id}/assignments`. `type` `PERSON` (`activityId`, `personId`), `VEHICLE` (`activityId`, `vehicleId`), `INSTRUMENT` (`activityId`, `instrumentId`) o `CONSUMABLE` (`activityId`, `consumableId`, `quantity` desde 1). `PUT /expeditions/{id}/permits/{permitId}` responde 204; repetirlo, o repetir el `PUT` de una certificación o de una precedencia, es 422 (el vínculo ya está: `duplicate permit`, `person already holds certification`, `duplicate predecessor`). `GET /expeditions/{id}/assignment-suggestions` devuelve la lista.

`POST /expeditions/{id}/submission` pasa a revisión. `DELETE` de esa ruta vuelve a borrador. `GET /expeditions/{id}/validation`: `{expeditionId, version, issues[]}`, cada issue con `severity`, `code` y `message`. `POST /expeditions/{id}/accepted-warnings`: `{issue, justification, acceptedBy}`; el issue es el de la validación vigente. `POST /expeditions/{id}/approval`.

`GET /expeditions/{id}/estimate`: `{duration, risk, consumption}`.

### Corrida

`POST /expeditions/{id}/run` la inicia. `GET` devuelve `{expeditionId, inForceId, status, activities, incidents, observations}`; sin corrida, 404. `status`: `IN_PROGRESS`, `SUSPENDED`, `FINISHED`. `POST /expeditions/{id}/run/suspension` suspende y `DELETE` de esa ruta reanuda. `POST /expeditions/{id}/run/completion` termina. `POST /expeditions/{id}/run/activities/{activityId}/initiation` la arranca. `POST .../activities/{activityId}/completion` recibe `{result, consumption}`; `consumption` (id de consumible a cantidad, desde 1) es opcional y cada actividad terminada lo devuelve en la corrida. `POST /expeditions/{id}/run/observations` recibe `{text}`. `GET /expeditions/{id}/report` devuelve `{status, plannedActivities, startedActivities, finishedActivities, duration, risk, risks, consumption, estimatedConsumption, incidents, observations, activityResults}`; `risks` es `[{activityId, risk}]`, de mayor a menor.

### Incidentes y replanificación

`POST /expeditions/{id}/incidents` recibe `{description, activityId}`, con `activityId` opcional. Responde 201, `{id}` y `Location` `/v1/expeditions/{id}/incidents/{incidentId}`, o los errores de siempre: 404 si el plan no existe, 409 sin corrida en curso, 422 si la actividad o la descripción no valen.

**Consistencia eventual.** La respuesta confirma que el incidente quedó registrado; la propuesta se prepara después, fuera del request. Si `activityId` es una actividad del plan vigente aprobado, el consumidor `replan-on-incident` crea una sola propuesta sobre una revisión de ese plan. Un fallo del consumidor no deshace el incidente: se reintenta con espera creciente y, si se agotan los intentos, la entrega queda `FAILED` hasta un reintento manual.

`GET /expeditions/{id}/incidents/{incidentId}` devuelve `{id, description, at, activityId?, proposalId?, processing}`; 404 si no existe la corrida o el incidente. `processing` es `[{consumer, status, attempts, lastAttemptAt?, lastError?}]`, con los estados `PENDING`, `RETRYING`, `DELIVERED` y `FAILED`. Un `processing` vacío es válido: el incidente no afecta una actividad o todavía no se repartió.

Para esperar la propuesta, consultar este `GET` hasta que **haya `proposalId`** o **todas las entregas de `processing` estén en un estado terminal** (`DELIVERED` o `FAILED`). Una entrega `DELIVERED` sin `proposalId` es un resultado final: el plan vigente cambió antes de procesar el incidente, ya no está aprobado o ya no incluye la actividad, y no corresponde una propuesta. Con `processing` vacío y sin `activityId` tampoco hay nada que esperar.

`POST /expeditions/{id}/revisions` responde 201, `{id}` y `Location` del borrador. En ese borrador, `DELETE /expeditions/{id}/activities/{activityId}` cancela, `POST .../activities/{activityId}/delays` recibe `{delay}`, una duración ISO-8601 positiva (cero o negativa: 422) y `POST /expeditions/{id}/replacements` reemplaza lo que no está disponible.

`GET /expeditions/{id}/replan-proposals` pagina; 404 si el plan no existe. `GET /replan-proposals/{id}` trae el incidente, la decisión (`PENDING`, `ACCEPTED`, `REJECTED`), quién decidió, cuándo y el plan sugerido. `POST /replan-proposals/{id}/acceptance` y `POST .../rejection` reciben `{responsible}`.

### Eventos

Cada hecho del negocio queda registrado, en el mismo momento y en la misma transacción que el cambio que lo causó: la aprobación de un plan, el inicio y el fin de la corrida, cada actividad iniciada y terminada, los incidentes, las propuestas de replanificación y su decisión. El registro solo se agrega: un evento no se modifica ni se borra. Solo se lee.

- `GET /expeditions/{id}/events` devuelve el historial del plan: los eventos del original, de todas sus revisiones (también borradores) y de sus propuestas, en orden. Consultar por el id de una revisión da el mismo resultado que por el del original. 404 si el plan no existe.
- `GET /events` devuelve los eventos de todos los planes.
- `GET /events/{eventId}` devuelve uno. 404 si no existe.
- `POST /events/{eventId}/deliveries/{consumer}/retry` vuelve a poner en cola una entrega `FAILED`: responde 204, y el despachador la intenta en la próxima pasada con los intentos en cero. 404 si la entrega no existe, 409 si no está `FAILED`.

Los dos primeros paginan (`page`, `size`, `Link`, como el resto) en orden ascendente de `sequence` y aceptan `type`, que se puede repetir (`?type=RunStarted&type=RunFinished`) y filtra por tipo. Un `type` que no es uno de los ocho nombres de abajo, o un `page` o `size` inválidos, responden 400.

`EventResponse`:

| Campo | Tipo | Nota |
|---|---|---|
| `id` | UUID | Identidad del evento registrado |
| `sequence` | número | Orden global: crece con cada evento y no se repite |
| `type` | string | Uno de los ocho tipos de abajo |
| `expeditionId` | UUID | Plan sobre el que ocurrió: el aprobado, o el original para la propuesta, y la raíz para la corrida |
| `occurredAt` | ISO-8601 | Cuándo ocurrió el hecho |
| `recordedAt` | ISO-8601 | Cuándo se registró |
| `payload` | objeto | Depende de `type`; una clave opcional ausente no se envía |
| `deliveries` | lista | `[{consumer, status, attempts, lastAttemptAt?, lastError?}]`: una por consumidor suscripto. Vacía si el evento no tiene suscriptores o todavía no se repartió. `lastError` es el último error observado (clase y mensaje, sin stack trace) y se conserva aunque un intento posterior tenga éxito |

| `type` | `payload` |
|---|---|
| `ExpeditionApproved` | `{version, supersededId?}`: `supersededId` es el plan que reemplaza, si es una revisión |
| `RunStarted` | `{}` |
| `ActivityStarted` | `{activityId}` |
| `ActivityCompleted` | `{activityId, startedAt, result, consumed}`: `consumed` es `{consumableId: cantidad}` |
| `IncidentReported` | `{incidentId, activityId?, description}` |
| `RunFinished` | `{}` |
| `ReplanProposed` | `{proposalId, suggestedId, incidentId}` |
| `ReplanDecided` | `{proposalId, decision, decidedBy}`: `decision` es `ACCEPTED` o `REJECTED` |

Ejemplo: `GET /v1/events?type=ReplanDecided&size=1`

```json
{
  "items": [{
    "id": "54997468-1be8-4b6e-b699-d745e16835eb",
    "sequence": 5,
    "type": "ReplanDecided",
    "expeditionId": "6a106ecf-6f82-417b-ae90-3712d19d5969",
    "occurredAt": "2026-11-09T00:22:33.201Z",
    "recordedAt": "2026-11-09T00:22:33.205Z",
    "payload": {"proposalId": "…", "decision": "ACCEPTED", "decidedBy": "…"},
    "deliveries": []
  }],
  "page": 0, "size": 1, "totalItems": 1, "totalPages": 1
}
```

Errores: 404 plan o evento desconocido; 400 `page`, `size` o `type` inválidos o un UUID mal formado. Con `memory` el registro no sobrevive al cierre de la aplicación.
