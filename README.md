# FieldOps

Plataforma de expediciones científicas y operaciones de campo.

## Build

Requiere JDK 25. El Maven Wrapper baja Maven 3.9.11 la primera vez.

```
./mvnw verify
```

En Windows (cmd o PowerShell): `mvnw.cmd verify`.

## Ejecutar

### Backend

```
./mvnw package -DskipTests
java -jar frameworks/target/fieldops-frameworks-1.0-SNAPSHOT.jar
```

La API queda en `http://localhost:8080/v1`. Otro puerto: `--server.port=9090`.

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
