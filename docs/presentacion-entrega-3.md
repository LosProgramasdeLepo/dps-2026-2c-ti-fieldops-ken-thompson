# Presentación de la Entrega 3

Presenta un integrante elegido al azar, así que todos ensayan la presentación completa. Reparto del tiempo: dominio ~30 %, propuesta de valor y demo ~40 %, decisiones ~20 %, margen ~10 %. Las decisiones citadas (D1.x a D4.x) están en `DESIGN.md`.

## Preparación

1. `docker compose up -d` (PostgreSQL y MongoDB).
2. Levantar el config server de la materia (`dynamic-config-server`, puerto 8888).
3. Backend con datos de demo, desde cero:

   ```
   docker compose down -v && docker compose up -d
   ./mvnw package -DskipTests
   java -jar frameworks/target/fieldops-frameworks-1.0-SNAPSHOT.jar --spring.profiles.active=sql,demo
   ```

4. Frontend: `cd frontend && npm install && npm run dev`, en `http://localhost:5173`.

Para la demo con MongoDB, lo mismo con `--spring.profiles.active=mongo,demo`.

## Guion de la demo

| Paso | Acción | Qué se muestra |
|---|---|---|
| 1 | Abrir el relevamiento aprobado ("Survey the biodiversity of the Delta wetlands"), pestaña "Eventos y auditoría" | Los eventos de la demo: plan aprobado, corrida iniciada, incidente y propuesta, cada uno con sus entregas `Entregado` |
| 2 | Abrir el plan nocturno en borrador ("Watch nocturnal raptors on the ridge"). En Recursos, adjuntar el permiso nocturno y aplicar las dos sugerencias (persona e iluminación). Enviar a revisión y aprobar | La aprobación es sincrónica: responde en el momento. En auditoría aparece `Plan aprobado` con `Aviso a responsables` entregado. En Personas, Bruno Diaz tiene la notificación |
| 3 | Volver al relevamiento, pestaña Ejecución. Registrar un incidente sobre una actividad | 201 inmediato y "Propuesta en preparación…"; en un segundo aparece el enlace a la propuesta. En auditoría, el `Incidente registrado` tiene dos entregas, `Propuesta de replanificación` y `Aviso a responsables`: consistencia eventual |
| 4 | En el config server, `fieldops.notifications.simulate-failure=true` para la aplicación `fieldops`; aplicar con `curl -X POST http://localhost:8080/actuator/refresh`. Registrar otro incidente | La propuesta se crea igual. `Aviso a responsables` pasa a `Reintentando` con el error y, en unos 15 s (5 intentos con espera de 1, 2, 4 y 8 s), a `Fallido`: fallos aislados |
| 5 | Volver a `false` y aplicar el refresh. En auditoría, "Reintentar" la entrega fallida | `Fallido` → `Pendiente` → `Entregado`, y la bandeja del responsable tiene una sola notificación de ese incidente: reintento sin duplicados |
| 6 | Pestaña Replanificación: aceptar la propuesta | `Propuesta decidida` en la auditoría, junto al original, el incidente y la propuesta |

Si el config server no está disponible, los pasos 4 y 5 se muestran con el test `NotificationUseCasesTest` (`aFailingNotificationRetriesWithoutStoppingTheReplan` y `turningTheFailureOffDeliversTheNotificationOnceAfterTheBackoff`), que usa el mismo evento de refresh.

Qué mostrar del código durante la demo:
- Quién emite: `ExpeditionExecution.addIncident` registra `IncidentReported`, y `RecordIncidentInteractor` lo anexa a `EventLog` en la misma transacción (D1.2, D2.1).
- Quién consume: `ReplanOnIncident` y `NotifyResponsiblesConsumer`, que solo traducen a un caso de uso (D3.7, D4.2).
- Qué garantías tiene la entrega: `EventDispatcher.dispatchDue`, con una fila por evento y consumidor, `REQUIRES_NEW` y `RetryPolicy` (D3.3 a D3.5).

## Preguntas probables

**¿Por qué no Kafka o RabbitMQ?** La consigna admite eventos en proceso. Un broker suma infraestructura, otra pieza en la CI y otra cosa para operar, y además necesita el outbox igual para no perder eventos entre el commit y el envío. Hoy alcanza una instancia. El camino de evolución es reemplazar la entrega local del despachador por un envío al broker, sin tocar a los productores (D3.1).

**¿Qué pasa si el proceso se cae entre el commit y el despacho?** Nada se pierde: el evento se guardó en la misma transacción que el agregado (outbox, D2.1). Al volver a arrancar sigue sin repartir o con entregas vencidas, y el despachador lo procesa en la próxima pasada. Si se cae a mitad de una entrega, esa transacción vuelve atrás y la entrega se reintenta.

**¿Cómo evitan duplicados?** La entrega es al menos una vez, así que cada consumidor es idempotente. La propuesta: una por incidente, como invariante del dominio (`Proposals.forIncident`) respaldada por `UNIQUE (incident_id)`. La notificación: una por evento y persona, por clave primaria. Además, en `sql` y `mongo`, el efecto y la marca de entregado van en la misma transacción (D3.4, D4.4).

**¿Qué orden garantizan?** Lo mejor posible por `sequence`, el orden global del registro. No es estricto: si un evento falla y espera su reintento, uno posterior se procesa antes. Se eligió así para que un fallo no frene a todos. Los consumidores toleran el desorden porque deciden con la hora del hecho y el estado actual: si el plan cambió, no crean la propuesta (D3.6, D3.10).

**¿Por qué la aprobación es sincrónica y la propuesta no?** La aprobación tiene que responder en el momento si el plan tiene críticos (409) y el usuario espera el resultado. La propuesta es costosa, puede fallar sola y la respuesta del incidente no la necesita: el incidente queda registrado igual (D3.8).

**¿Qué pasa con varias instancias?** Hoy hay una sola instancia del despachador (deuda, D3.11). Con varias habría que reclamar cada entrega antes de procesarla: `FOR UPDATE SKIP LOCKED` en PostgreSQL y `findAndModify` con un plazo en MongoDB (Competing Consumers). El refresh de la configuración también es por instancia.

**¿Cómo se agrega un evento o un consumidor?** Un evento: un record nuevo en el `sealed` de `DomainEvent`, registrado por el agregado; el compilador obliga a completar el codec y la API. Un consumidor: una clase que implementa `EventConsumer`, con un nombre estable, que traduce a un caso de uso. `notify-responsibles` se agregó así, sin tocar a ningún productor ni a `replan-on-incident`.

**¿Por qué `sealed`?** Para que los `switch` de los bordes (el codec de almacenamiento y el mapeo de la API) sean exhaustivos: un evento nuevo que falte en alguno no compila (D1.1).

**¿Esto es Event Sourcing?** No. El estado sigue en los agregados y el registro no los reconstruye: es un outbox que además sirve de auditoría (D2.2).

**¿Dónde está la regla de quién recibe una notificación?** En el dominio, en `ResponsiblesToNotify`: los responsables del charter del plan vigente, buscado con `Revisions.inForce`. El consumidor no la conoce, y `ArchitectureTest` impide que `adapters.messaging` dependa del modelo (D4.2).

**¿Qué pasa si falla un consumidor?** Su entrega pasa a `RETRYING` con espera creciente y, al agotar los intentos, a `FAILED`, visible en la auditoría con el último error. El otro consumidor del mismo evento no se entera y la operación original no se deshace. Una entrega `FAILED` se reintenta a mano (D3.5).

**¿Cómo lo probaron?** Emisión: cada caso de uso anexa su evento (tests de casos de uso). Efecto: al despachar hay una propuesta y una notificación por responsable. Fallo: un consumidor que lanza deja la entrega en `RETRYING` sin propuesta, se recupera tras la espera y, si falla siempre, queda `FAILED` hasta el reintento manual. Asincronía real: `AsyncDispatchTest` responde 201 con un consumidor bloqueado. Persistencia: los mismos flujos sobre PostgreSQL y MongoDB con Testcontainers.
