# FieldOps

Plataforma de expediciones científicas y operaciones de campo.

## Build

Requiere JDK 25. El Maven Wrapper baja Maven 3.9.11 la primera vez.

```
./mvnw verify
```

En Windows (cmd o PowerShell): `mvnw.cmd verify`.

## Ejecutar

```
./mvnw package -DskipTests
java -jar frameworks/target/fieldops-frameworks-1.0-SNAPSHOT.jar
```

La API queda en `http://localhost:8080/v1`. Otro puerto: `--server.port=9090`.
