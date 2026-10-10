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
