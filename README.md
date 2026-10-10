# FieldOps

Plataforma de expediciones científicas y operaciones de campo.

## Build

Requiere JDK 25. El Maven Wrapper baja Maven 3.9.11 la primera vez.

```
./mvnw verify
```

En Windows (cmd o PowerShell): `mvnw.cmd verify`.

Los tests de persistencia levantan PostgreSQL con Testcontainers, así que `verify` necesita Docker corriendo.

## Ejecutar

### Backend

El backend guarda en PostgreSQL. `compose.yaml` levanta uno local (base, usuario y contraseña `fieldops`, puerto 5432):

```
docker compose up -d
./mvnw package -DskipTests
java -jar frameworks/target/fieldops-frameworks-1.0-SNAPSHOT.jar
```

Flyway crea o actualiza el esquema al arrancar. La API queda en `http://localhost:8080/v1`. Otro puerto: `--server.port=9090`. Otra base: `FIELDOPS_DB_URL`, `FIELDOPS_DB_USER` y `FIELDOPS_DB_PASSWORD`.

Perfiles:

- `--spring.profiles.active=demo` carga los datos de demostración si no hay planes: un relevamiento aprobado con doce actividades de los seis tipos, un bloque compuesto, recursos compartidos, una advertencia aceptada, la corrida iniciada y una propuesta de replanificación por un incidente, y un plan en borrador con errores críticos. Se puede repetir: si ya hay planes, no carga nada.
- `--spring.profiles.active=memory` guarda en memoria y no necesita base; los datos se pierden al cerrar.

Para empezar de cero: `docker compose down -v`.

### Frontend

Requiere Node 22.

```
cd frontend
npm install
npm run dev
```

La aplicación queda en `http://localhost:5173` y redirige `/v1` al backend. Si el backend corre en otro puerto: `FIELDOPS_API=http://localhost:9090 npm run dev`.

`npm run build` verifica los tipos y genera `frontend/dist`; `npm run preview` la sirve con el mismo redireccionamiento.

Los instantes se cargan y se muestran en UTC.

## Contrato de la API

`http://localhost:8080/v1`, JSON. Instantes y duraciones en ISO-8601 (`PT2H`). Un período es `{start, end}`. Jackson rechaza propiedades que no conoce. Un alta responde 201 con `Location` y `{id}`. Las demás escrituras responden 204. Los errores son `application/problem+json` (RFC 9457).

Paginan, en orden de alta, `GET /expeditions`, `/people`, `/certifications`, `/vehicles`, `/instruments`, `/consumables`, `/permits` y `/expeditions/{id}/replan-proposals`. `page` desde 0 (por defecto 0), `size` de 1 a 100 (por defecto 20). Cuerpo: `{items, page, size, totalItems, totalPages}`. `Link` (RFC 8288): `first`, `prev` si `page` > 0, `next` si no es la última, `last`. Las sugerencias de asignación y la validación no paginan.

- JSON mal formado, UUID inválido, body que no pasa la validación, o `page`/`size` inválidos: 400
- id del path desconocido: 404. `GET /expeditions/{desconocido}` es 404
- referencia del body desconocida, valor inválido, itinerario o asignación inválidos, u otra regla de dominio: 422. `POST /expeditions` con un responsable inexistente es 422
- transición de plan o de corrida inválida, o plan que no se puede aprobar: 409
- cualquier otro error: 500, detalle genérico, sin mensaje interno ni stack trace

### Catálogo

`POST` crea y `GET` lista o lee uno.

- `/certifications`: `{name}`
- `/people`: `{name, certifications[], availability}`. `PUT /people/{id}/certifications/{certificationId}`. `PUT /people/{id}/availability` con `{availability}`
- `/vehicles`: `{capacity, availability}`. `PUT /vehicles/{id}/availability`
- `/instruments`: `{kind, availability}`. `PUT /instruments/{id}/availability`
- `/consumables`: `{name, stock}`. `PUT /consumables/{id}/stock` con `{stock}`
- `/permits`: `{kind, zone, validity}`

`availability` es `{periods: [{start, end}]}`. El `kind` de un permiso es `zone` o `night operation`. `capacity` y `stock` son enteros positivos.

### Planes

`POST /expeditions` recibe `{objectives[], period, zones[], responsibles[], restrictions[]}`. `GET /expeditions` devuelve resúmenes (`id`, `version`, `supersedes`, `status`, `charter`). `GET /expeditions/{id}` devuelve charter, itinerario, asignaciones, permisos y advertencias aceptadas. `status`: `DRAFT`, `IN_REVIEW`, `APPROVED`, `SUPERSEDED`.

`POST /expeditions/{id}/activities`. `Location`: `/v1/expeditions/{id}/activities/{activityId}`. `kind`: `SAMPLING`, `MEASUREMENT`, `TRANSIT`, `NIGHT`, `DIVE`, `CAMP`. Comunes: `name`, `estimatedDuration`, `risk` (`LOW`, `MEDIUM`, `HIGH`), `consumption` (id de consumible a cantidad, desde 0), `zone`, `window`, `predecessors[]`. `SAMPLING`, `NIGHT` y `DIVE` agregan `certification`. `MEASUREMENT` agrega `certification` e `instrument`. `TRANSIT` y `CAMP` no agregan campos. `NIGHT` pide además iluminación y permiso nocturno, y el riesgo sube un nivel. El `GET` no devuelve `kind`: devuelve `requirements` (`certifications`, `heldByEveryone`, `instruments`, `specialPermits`, `vehicles`).

`POST /expeditions/{id}/blocks` responde 201 con el bloque y `Location` `/v1/expeditions/{id}`. `node` `BLOCK`: `arrangement` `SEQUENTIAL` o `PARALLEL`, y `parts` (al menos dos). `node` `ACTIVITY`: `activity` con la forma de arriba.

`PUT /expeditions/{id}/activities/{activityId}/predecessors/{predecessorId}` agrega una precedencia. `POST /expeditions/{id}/assignments`. `type` `PERSON` (`activityId`, `personId`), `VEHICLE` (`activityId`, `vehicleId`), `INSTRUMENT` (`activityId`, `instrumentId`) o `CONSUMABLE` (`activityId`, `consumableId`, `quantity` desde 1). `PUT /expeditions/{id}/permits/{permitId}`. `GET /expeditions/{id}/assignment-suggestions` devuelve la lista.

`POST /expeditions/{id}/submission` pasa a revisión. `DELETE` de esa ruta vuelve a borrador. `GET /expeditions/{id}/validation`: `{expeditionId, version, issues[]}`, cada issue con `severity`, `code` y `message`. `POST /expeditions/{id}/accepted-warnings`: `{issue, justification, acceptedBy}`; el issue es el de la validación vigente. `POST /expeditions/{id}/approval`.

`GET /expeditions/{id}/estimate`: `{duration, risk, consumption}`.

### Corrida

`POST /expeditions/{id}/run` la inicia. `GET` devuelve `{expeditionId, inForceId, status, activities, incidents, observations}`; sin corrida, 404. `status`: `IN_PROGRESS`, `SUSPENDED`, `FINISHED`. `POST /expeditions/{id}/run/suspension` suspende y `DELETE` de esa ruta reanuda. `POST /expeditions/{id}/run/completion` termina. `POST /expeditions/{id}/run/activities` recibe `{activityId}`. `POST .../activities/{activityId}/completion` recibe `{result}`. `POST /expeditions/{id}/run/observations` recibe `{text}`. `GET /expeditions/{id}/report`.

### Incidentes y replanificación

`POST /expeditions/{id}/incidents` recibe `{description, activityId}`. `activityId` es opcional; si viene, queda una propuesta. `POST /expeditions/{id}/revisions` responde 201, `{id}` y `Location` del borrador. En ese borrador, `DELETE /expeditions/{id}/activities/{activityId}` cancela, `POST .../activities/{activityId}/delay` recibe `{delay}` y `POST /expeditions/{id}/reassignments` reemplaza lo que no está disponible.

`GET /expeditions/{id}/replan-proposals` pagina; 404 si el plan no existe. `GET /replan-proposals/{id}` trae el incidente, la decisión (`PENDING`, `ACCEPTED`, `REJECTED`), quién decidió, cuándo y el plan sugerido. `POST /replan-proposals/{id}/acceptance` y `POST .../rejection` reciben `{responsible}`.
