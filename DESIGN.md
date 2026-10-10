# Decisiones de diseño

## Aplicadas

**Arquitectura.** Clean Architecture en cinco módulos Maven, con dependencias solo hacia adentro: `frameworks → api → application → domain` y `frameworks → adapters → application → domain`. `api` no depende de `adapters`. `domain` guarda el modelo, los servicios de dominio (`Replanner`, `AssignmentSuggester`, `RuleBasedValidator`) y los puertos de lectura (`People`, `Vehicles`, `Instruments`, `Consumables`, `Permits`, `Certifications`, `Catalogs`). `application` guarda las interfaces de caso de uso, los interactors, los snapshots, `PlanningContexts`, los repositorios, `Clock` y los `*Registry` de escritura, que extienden el puerto de lectura. `adapters` implementa esos puertos dos veces, en memoria (`ResourceCatalog`, repositorios `InMemory*`) y con JPA sobre PostgreSQL (`adapters.jpa`), más los relojes (`SystemClock`, `FixedClock`), y no nombra interactors. `frameworks` es la raíz de composición (`FieldOps`, `FieldOpsRepositories`, `FieldOpsApplication` y la variante `InMemoryFieldOps`) y los tests que manejan el sistema. Dentro de `domain` los paquetes siguen el componente: `validation → expedition → {tracking, itinerary, catalog, assessment}`, `report → {expedition, tracking}`, `tracking → {identity, shared}`. Las transiciones del agregado, y las operaciones de `Replanner` y `ReplanProposal` que los casos de uso invocan, son públicas y conservan su precondición. La política de aprobación la ejecuta `Expedition.approve`; `ApproveExpeditionInteractor` arma el contexto y persiste. `ExpeditionExecution` sigue en `expedition` porque es el agregado de la corrida. Los ids salen de los repositorios. El tiempo sale de `Clock`.

**Validador como puerto.** `ExpeditionValidator` es una interfaz de `expedition` y `RuleBasedValidator`, en `validation`, la implementa. Tiene una sola implementación porque existe para cortar el ciclo entre esos paquetes (DIP).

**Casos de uso.** `AdministerPersonnel`, `AdministerEquipment`, `AdministerPermits`, `ConsultPersonnel`, `ConsultEquipment`, `ConsultPermits`, `DraftExpedition`, `PlanItinerary`, `AssignResources`, `ReviewExpedition`, `ApproveExpedition`, `TrackExpedition`, `RecordIncident`, `ReplanExpedition`, `ReviewReplanProposal`, `ConsultExpedition`, `EstimateExpedition`, `ReportExpedition`. Cada uno agrupa las operaciones de un actor; seguimiento e incidentes están separados porque cambian por motivos distintos, y también la consulta del catálogo de su administración: quien planifica lee el catálogo para asignar y no lo modifica. Los que modifican cargan por id, aplican y guardan; los de lectura no guardan. Ninguna entidad mutable sale: las consultas devuelven `PlanSnapshot`, `ProposalSnapshot`, `RunSnapshot`, `Estimate`, `OperationalReport` y `ValidationResult`, armados por el interactor; los recursos del catálogo salen como entidades porque son inmutables. Entran ids y objetos inmutables. Las colecciones se leen por página con `PageRequest` y `Page`, de `usecase.shared`: paginar es una necesidad de la aplicación, no una regla del dominio.

**Puertos.** Lectura por recurso (`People`, `Vehicles`, `Instruments`, `Consumables`, `Permits`, `Certifications`), los reservables y de validación agrupados en `BookableResources` y `Catalogs`, en `domain`, porque las reglas y el sugeridor los usan. La escritura también es por recurso y vive en `application`: cada `*Registry` extiende su puerto de lectura con el próximo id, `save` (upsert), el listado por página y un `require` que lanza `UnknownResource`, así la validación no depende de métodos de escritura y los interactors no repiten la búsqueda. `ExpeditionRepository` (con `require`, los ids de actividad y `all(PageRequest)`), `ExecutionRepository`, `ReplanProposalRepository` (con las propuestas de un plan por página) y `Clock` también están en `application`. Los adaptadores en memoria, los de JPA y los relojes están en `adapters`.

**Clean Code.** Hasta tres argumentos. Excepciones: records (`OperationalReport`, `PlanSnapshot`, `ExpeditionCharter`, `ResourceRequirements`, `Permit`, `ExpeditionState`, `ExecutionState`, `FieldOpsRepositories`), constructores de interactors (hasta seis colaboradores en `RecordIncidentInteractor`), `Person` y el bean que cablea `FieldOpsRepositories`. `ExpeditionCharter` y `PlanningContext` agrupan argumentos que viajan juntos; `Activity` se arma con un builder por tipo. Sin banderas booleanas: `ActivityBlock.Arrangement` en vez de `boolean parallel`, `overlapsWithin` y `overlapsBetween` en vez de un `conflicts` con flag. Sin `null` en parámetros ni retornos: `Incident` guarda la actividad como `Optional` y `OperationalReport.of` tiene una versión con corrida y otra sin. CQS: `Assignments.withdraw` no devuelve nada; en `ReplanExpedition` solo `revise` crea y devuelve un id, y los cambios sobre el borrador no devuelven nada. Un término por concepto (`occupying`, `inForce`, `lineage`). Sin comentarios.

**Dos agregados.** `Expedition` es el plan (`DRAFT | IN_REVIEW | APPROVED | SUPERSEDED`) y `ExpeditionExecution` la corrida (`IN_PROGRESS | SUSPENDED | FINISHED`). `OperationalStatus` reúne los seis estados. Solo `DRAFT` se edita. Dentro del plan, `Expedition` tiene la identidad, la versión, el estado y las advertencias aceptadas, y `PlanContent` tiene charter, itinerario, asignaciones y permisos con las reglas que los cruzan (zona y período de cada actividad, asignación sobre una actividad existente, retraso dentro del período): cambian por motivos distintos, el flujo de revisión y la planificación. `Expedition` verifica el estado y delega; su API pública no expone `PlanContent`.

**Aprobación.** `Expedition.approve` exige `IN_REVIEW`, llama al `ExpeditionValidator` con el `PlanningContext` de ese plan y rechaza con `ExpeditionNotApprovable` si hay críticos o advertencias sin aceptar. No recibe un `ValidationResult`. Si el predecesor no puede pasar a `SUPERSEDED`, la revisión sigue en `IN_REVIEW`. `submit` rechaza un itinerario vacío; los críticos los rechaza `ReviewExpedition`. Una advertencia la acepta un responsable, con justificación, solo si la validación actual la produce; volver a borrador las borra.

**Revisiones.** Un plan aprobado no se edita. `reviseAsDraft(id)` crea una revisión (versión + 1, `supersedes`) con una copia del `PlanContent`, sin advertencias. La revisión rige cuando se aprueba: la aprobación marca `SUPERSEDED` al predecesor, que tiene que seguir `APPROVED`, y por eso se aprueba una sola revisión por plan. Hasta entonces el original sigue vigente y ocupando.

**Corrida y linaje.** La corrida vive en la raíz. `Revisions.inForce` pasa de un `SUPERSEDED` a su revisión aprobada: ese itinerario sigue el seguimiento. Ningún plan del linaje arranca otra corrida. Ocupan recursos los planes `IN_REVIEW` o `APPROVED` cuya corrida no terminó y los `SUPERSEDED` con corrida abierta. `OccupyingExpeditions` excluye el linaje del plan evaluado.

**Actividad.** Duración, riesgo y consumo estimado son datos. El tipo fija los requisitos (`ResourceRequirements`): certificaciones que alguien debe tener, las que debe tener cada asignado (`heldByEveryone`), tipos de instrumento, tipos de permiso especial (`PermitKind`) y cantidad de vehículos. Son conjuntos y cantidades, no banderas: las reglas y el sugeridor recorren lo pedido sin preguntar si hace falta. Seis tipos: muestreo, medición, traslado, nocturna, buceo, campamento. Un tipo nuevo es otra fábrica; reglas, estimación e informes no preguntan el tipo.

**Itinerario.** Composite: in`ItineraryItem` es `Activity` o `ActivityBlock`. Es `sealed` porque hoja y compuesto es un conjunto cerrado; cada variante resuelve `blocks`, `precedence`, `replacing` y `without`, y no hay `switch` sobre el tipo. `Arrangement` decide qué agrega el orden (`order`) y qué partes corren a la vez (`concurrent`). `duration(Function)` suma en la secuencia y toma el máximo en paralelo. La raíz suma sin ordenar. En un bloque secuencial cada parte depende de todas las hojas de la anterior; esas dependencias se suman a las explícitas para validar ventanas y ciclos, para `delay` y para el seguimiento.

**Asignaciones.** `Assignments` guarda una lista por tipo y cada asignación se archiva sola (double dispatch), sin `instanceof`. `BookableAssignment` (persona, vehículo, instrumento) da el id del recurso (`BookableId`) y lo busca en el catálogo (`Bookable`); con eso `TemporalBooking` es un solo record que compara ids y ventanas y pregunta la disponibilidad, sin métodos por tipo. El consumible solo declara una cantidad positiva. `Expedition.assignments()` devuelve una copia.

**Catálogo.** Tres casos de uso, uno por actor: `AdministerPersonnel` da de alta certificaciones y personas, certifica con certificaciones registradas y cambia la disponibilidad de las personas; `AdministerEquipment` da de alta vehículos, instrumentos y consumibles, cambia su disponibilidad y el stock; `AdministerPermits` da de alta permisos. Entidades inmutables (`withAvailability`, `certified`, `withStock`). El permiso tiene un `PermitKind` (`ZONE`, `NIGHT`) y cubre zona y vigencia; un tipo de permiso nuevo es otra constante.

**Validación.** Strategy: `RuleBasedValidator` recorre las reglas que recibe y `withDefaultRules` arma las siete. Otra regla es otra clase (`ValidationExtensionTest`). Todas reciben `PlanningContext`.

**Códigos.** `RESOURCE`: id desconocido (asignación o permiso) o actividad sin la persona, el vehículo o el instrumento que pide. `CERTIFICATION`: ninguna persona conocida tiene la certificación pedida a alguien, o alguna no tiene la pedida a todos. `PERMIT`: actividad sin un permiso conocido que la cubra, o sin un permiso de cada tipo especial que pide; sin adjuntos quedan todas descubiertas, y si todos los adjuntos son desconocidos solo sale `RESOURCE`. `OVERLAP`: recurso en ventanas superpuestas, propias o de las que ocupan. `AVAILABILITY`: recurso conocido fuera de su disponibilidad. `PARALLEL`: recurso en dos ramas de un bloque paralelo, con cualquier ventana. `STOCK`: lo asignado, propio y de las que ocupan, supera el depósito. `CAPACITY` es `WARNING`; el resto, `CRITICAL`. Un id ya en `RESOURCE` no se evalúa por disponibilidad, stock, certificación ni capacidad. Los códigos son `String` para que una regla nueva no toque un enum.

**Sugerencias.** `AssignmentSuggester` cubre vehículo, instrumento del tipo pedido y personas con lo que se exige a todos. Elige el primer recurso disponible y libre frente a las reservas propias y de las que ocupan. No modifica el plan.

**Replan.** `ReplanExpedition.revise` crea la revisión en borrador de un plan `APPROVED`; `cancel`, `delay` y `replaceUnavailable` solo cambian borradores, así que un plan `IN_REVIEW` vuelve primero a borrador con `ReviewExpedition.returnToDraft`. Cada operación tiene un solo efecto y el id del plan no cambia según el estado. `cancel` quita la actividad y las dependencias hacia ella. `delay` corre la actividad y empuja a sus dependientes; se rechaza si alguna ventana sale del período. `replaceUnavailable` suelta reservas no disponibles o en conflicto. Las tres rellenan con el sugeridor. `respondTo` elige la respuesta a un incidente.

**Seguimiento.** Una actividad arranca dentro de su ventana y con sus predecesores, explícitos y de secuencia, terminados. Se cierra después de su inicio aunque pase la ventana. La expedición termina cuando está terminado lo iniciado y lo del plan vigente. Incidentes y observaciones se registran con la corrida en curso o suspendida. `Clock` sella los instantes. La consulta de la corrida informa el plan vigente (`RunSnapshot.inForce`), para que el seguimiento muestre las actividades de la revisión que rige.

**Estimación e informe.** `Estimate`: duración del árbol, riesgo máximo (`LOW` sin actividades) y consumo estimado de las actividades. `OperationalReport` sin corrida informa la duración estimada y el consumo asignado; con corrida, la duración medida combinada en el árbol, el consumo asignado a lo terminado, los incidentes, las observaciones y los resultados.

**Invariantes.** El charter exige objetivos, zonas y responsables, y los responsables tienen que existir en el catálogo. La ventana alcanza la duración estimada. Los predecesores existen, no forman ciclos y terminan antes. La zona es de la expedición y la ventana está dentro del período. `TimePeriod`, `Stock`, `Passengers`, `WorkZone` y `ExpeditionCharter` se validan al construirse.

**Errores.** Todo extiende `DomainException`. `UnknownResource`: id desconocido de un recurso nombrado (expedición, persona, certificación, vehículo, instrumento, consumible, permiso, responsable, propuesta). `InvalidValue`: dato inválido, no responsable, advertencia inexistente, duplicado o propuesta ya decidida. `InvalidItinerary`: grafo, ventanas y bloques. `InvalidAssignment`: asignación repetida, desconocida o sin cantidad. `InvalidActivityExecution`: seguimiento ilegal. `InvalidExpeditionTransition`: transición de estado ilegal del plan o de la corrida. `ExpeditionNotApprovable`: críticos o advertencias sin justificar. Un nulo en un constructor o un paso faltante del builder lanza `NullPointerException`.

**Tests.** Unitarios por componente, cada uno con lo mínimo que la regla necesita: el seguimiento se prueba sin armar un plan y el informe sin aprobarlo. Los tests de entidades llaman las operaciones del agregado y arman el catálogo con un doble propio, sin depender de `adapters`. Las reglas del contenido se prueban en `PlanContentTest` y las del estado y las revisiones en `ExpeditionTest`. Los casos de uso se prueban por grupo (`CatalogUseCasesTest`, `PlanningUseCasesTest`, `ReviewUseCasesTest`, `TrackingUseCasesTest`, `ReplanningUseCasesTest`) sobre `UseCaseFixture`, que toma el sistema cableado por `InMemoryFieldOps`. La aprobación se prueba en `ExpeditionTest`; el caso de uso, en `ReviewUseCasesTest`. La restauración de cada agregado se prueba en su componente: ida y vuelta e invariantes rotos. La API se prueba con un `@WebMvcTest` por controlador y un `@SpringBootTest` del flujo sobre los adaptadores en memoria (perfil `memory`), que además lee cada `Location` que devuelve un alta. Los repositorios JPA se prueban contra PostgreSQL con Testcontainers (`@JpaRepositoryTest`: `@DataJpaTest` con `ddl-auto=validate` sobre el esquema de Flyway): ida y vuelta de cada agregado comparando todos sus datos, upsert, orden de alta, páginas e integridad referencial. Como el test hace rollback, `SET CONSTRAINTS ALL IMMEDIATE` verifica las restricciones diferidas antes de releer. `PersistentFlowTest` recorre por HTTP, sobre PostgreSQL, planificación, aprobación, corrida, incidente y aceptación de la propuesta. `TransactionalUseCaseTest` prueba el decorador y `DemoDataTest` los datos de demostración. `PaginationTest` fija el contrato de página (valores por defecto, `Link` y parámetros inválidos), `PageRequestTest` y `PageTest` los límites y el conteo de páginas, e `InMemoryPagesTest` el recorte en memoria. `ArchitectureTest` fija que `domain` no depende de `usecase`, `adapters`, `api` ni `frameworks`, que `application` (`usecase`) no depende de `adapters`, `api` ni `frameworks`, que `api` no depende de `adapters`, y que `domain` y `usecase` no dependen de `jakarta.persistence`, Hibernate ni Spring.

## Descartadas

**Política por tipo o herencia de** `Activity`**.** Los tipos difieren en datos. Un tipo nuevo es una fábrica.

**Supertipo de todos los ids o de las cantidades.** Mezclaría stock con pasajeros. Solo los ids reservables comparten `BookableId`, que no confunde una persona con un vehículo porque la igualdad es por tipo. El costo son métodos paralelos por tipo en `Assignments`.

**State por estado.** `ExpeditionStatus` solo responde `occupiesResources` y las transiciones preguntan la constante. Un estado nuevo toca las transiciones del agregado.

**Aprobar un** `ValidationResult` **ya calculado.** Puede no describir el plan actual.

**Versionar el borrador.** `DRAFT` e `IN_REVIEW` se editan en el lugar.

**Reordenar una lista.** El orden sale de dependencias, ventanas y bloques secuenciales.

`switch` **sobre un** `Assignment` **sealed.** Lo reemplaza el double dispatch.

**Un interactor por operación y request models.** Se agrupa por actor y no sale ninguna entidad mutable; las entradas usan objetos de dominio inmutables. Los argumentos inmutables cruzan el borde del caso de uso; un DTO paralelo se reconstruiría en el mismo objeto.

**Códigos de issue como enum.** Cada regla nueva tocaría el enum; se escriben como `String`.

**`Pageable` de Spring Data.** Metería Spring en `application`. `PageRequest` y `Page` son de la aplicación y `PageQuery` los traduce en el borde HTTP.

**HAL o HATEOAS completo.** Solo la paginación lleva enlaces, en el header `Link`; el resto de las respuestas son records planos.

## API REST

`api` es el adaptador de entrada HTTP. Depende solo de `application`: no ve repositorios ni el reloj. `domain` y `application` no tienen anotaciones de Spring. `frameworks` arranca Spring Boot 4.1.1 (`FieldOpsApplication`) y una `@Configuration` arma `FieldOps` con los repositorios que elige `fieldops.persistence` y publica cada interfaz de caso de uso como bean, envuelta en su transacción. Los interactors siguen sin anotaciones. `SystemClock` implementa `Clock` con `Instant.now()`; `FixedClock` queda para los tests.

Un método HTTP por operación de caso de uso, bajo `/v1`. Rutas y cuerpos están en el README. Ningún controlador habla con un repositorio. Todo `Location` de un alta se lee con `GET`: el catálogo, el plan, la actividad (`/v1/expeditions/{id}/activities/{activityId}`) y la propuesta (`/v1/replan-proposals/{id}`). La corrida se lee en `GET /v1/expeditions/{id}/run`, con el id del plan vigente (`inForceId`); sin corrida, 404. El alta de un bloque responde 201 con el bloque y el `Location` del plan. El cliente no elige ids de actividad: el controlador pide `nextActivityId()` y lo devuelve. Aprobar es `POST /v1/expeditions/{id}/approval` y responde 204, no un parche de `status`, porque el agregado revalida y puede rechazar el plan.

El JSON son records de `api`. Los snapshots no se serializan: `PlanSnapshot` expone `Optional`, `ItineraryItem` y `Assignment`, y publicarlo ataría el contrato al modelo. `Activity` entra como unión discriminada por `kind` (`SAMPLING`, `MEASUREMENT`, `TRANSIT`, `NIGHT`, `DIVE`, `CAMP`); el mapper elige la fábrica, así que un tipo nuevo es otra variante y no un `switch` en el dominio. La respuesta muestra `ResourceRequirements`, no un tipo reconstruido. `ActivityBlock` viaja como árbol (`node` `ACTIVITY` o `BLOCK`, con `arrangement` y `parts`). `Assignment` es otra unión (`PERSON`, `VEHICLE`, `INSTRUMENT`, `CONSUMABLE`). `TimePeriod` es `{start, end}` en ISO-8601 y `Duration` también. Jackson rechaza propiedades desconocidas, y strings y listas tienen tope con `@Size`.

Paginan el catálogo, los planes y las propuestas de un plan, en orden de alta. El listado de planes devuelve resúmenes (`ExpeditionSummaryResponse`: identidad, versión, estado y charter) y el detalle sale del `GET` del plan. `PageQuery` valida `page` y `size` en el borde y `PageRequest` vuelve a exigir los límites en `application`, como `Stock` frente a `@Min`. Las sugerencias de asignación y la validación no paginan porque se calculan, no se guardan. Las propuestas de un plan pasaron de lista a página y responden 404 si el plan no existe.

Los errores salen como `application/problem+json` (RFC 9457) desde un `@RestControllerAdvice`. Los controladores no atrapan excepciones.

- JSON mal formado, UUID inválido, body que falla `@Valid` o `page`/`size` inválidos: 400
- `UnknownResource` cuyo id coincide con una variable del path: 404
- `UnknownResource` de una referencia del body, `InvalidValue`, `InvalidItinerary`, `InvalidAssignment`: 422
- `InvalidExpeditionTransition`, `ExpeditionNotApprovable`, `InvalidActivityExecution`: 409
- otro `DomainException`: 422
- cualquier otra: 500, con detalle genérico y sin mensaje interno ni stack trace

Así `GET /v1/expeditions/{desconocido}` es 404 y `POST /v1/expeditions` con un responsable inexistente es 422.

Deuda: no hay autenticación ni autorización. `ReviewExpedition.acceptWarning` y la decisión de `ReviewReplanProposal` reciben el `PersonId` del actor en el body, así que cualquiera puede declararse responsable. El actor tiene que salir de un principal autenticado, no del payload, antes de exponer la API fuera de la cursada.

## Frontend

`frontend/` es una SPA mínima (React, TypeScript y Vite) que solo habla con la API REST: no comparte código con el backend ni lo necesita para compilar. En desarrollo Vite redirige `/v1` al backend, así que el backend no habilita CORS. Cubre cada funcionalidad: el catálogo (altas, certificaciones, disponibilidad, stock y permisos), la planificación (charter, itinerario con bloques anidados y dependencias, asignaciones manuales y sugeridas, permisos), la revisión (validación, advertencias aceptadas con justificación, aprobación), el seguimiento (corrida, actividades, observaciones e incidentes), la replanificación (propuestas, revisiones, cancelar, retrasar y reasignar) y los informes.

`src/api` tipa el contrato y traduce `problem+json` a `ApiError`; las pantallas no llaman a `fetch`. Las reglas quedan en el backend: la interfaz habilita acciones según el estado del plan y muestra el error que devuelve la API. Los instantes se cargan y se muestran en UTC, como los guarda el dominio.

Deuda: no hay tests del frontend. Los selectores cargan el catálogo completo, de a páginas de 100. Una asignación cargada por error no se puede quitar de un borrador, porque la API no expone `Assignments.withdraw`.

## Persistencia

**Adaptador.** JPA (Spring Data y Hibernate) en `adapters.jpa`, por componente: `catalog`, `expedition` y `tracking`. Las entidades son del adaptador y se convierten desde y hacia el dominio; el dominio y `application` no tienen anotaciones. Cada adaptador implementa un puerto (`JpaPersonRegistry` y los demás `*Registry`, `JpaExpeditionRepository`, `JpaExecutionRepository`, `JpaReplanProposalRepository`) y abre o se une a una transacción con `TransactionOperations`, así la carga perezosa no depende de quién lo llama. `StoredCatalog` reúne buscar, listar, paginar y guardar del catálogo.

**Esquema.** PostgreSQL con Flyway (`V1__catalog`, `V2__expeditions`, `V3__runs`, `V4__replan_proposals`); Hibernate solo valida (`ddl-auto=validate`). Normalizado: una tabla por colección de cada agregado, con su posición cuando el orden importa. Claves foráneas al catálogo en asignaciones, permisos adjuntos, responsables, quien acepta una advertencia y quien decide una propuesta; al plan reemplazado en `supersedes_id`; y entre actividades del mismo plan en dependencias, nodos, requisitos, consumo y asignaciones. `CHECK` en estados, riesgos, decisiones, cantidades, períodos, la versión (`1` si y solo si no reemplaza a otro plan) y la decisión (quién y cuándo solo si no está pendiente). Las claves entre tablas de un plan son diferibles: Hibernate reescribe las colecciones en cualquier orden y la integridad se verifica al confirmar. Los textos son `text`: el largo lo limita la API, no la base.

**Itinerario.** `itinerary_nodes` guarda el árbol en preorden con la posición del bloque que lo contiene (`parent < position`, así no hay ciclos); un nodo es bloque (`arrangement`) o actividad (`activity_id`), nunca las dos. `ItineraryRows` aplana y reconstruye el árbol; `AssignmentRows` separa las asignaciones por tipo y conserva su orden. Igual que el mapper de `api`, el adaptador elige la variante en el borde con un `switch`.

**Restauración.** `Activity.restoring`, `Itinerary.restoring`, `Expedition.restore(ExpeditionState)` y `ExpeditionExecution.restore(ExecutionState)`, con `state()` como contraparte. Revalidan los invariantes de siempre y la coherencia del estado: versión y `supersedes`, sin advertencias en borrador, con actividades fuera de borrador, advertencias de un responsable, actividades sin repetir y una corrida terminada sin actividades abiertas. `Activity.restoring` toma el riesgo guardado sin reaplicar el factor nocturno, porque `raised` no se invierte. `Itinerary.restoring` valida el árbol entero, porque una dependencia puede apuntar a un ítem guardado después. `ReplanProposal` no cambió: se arma con su constructor y se decide con `accept` o `reject`, que reciben quién y cuándo.

**Registro.** `expeditions` guarda todo plan y `registered_expeditions` los que guardó `ExpeditionRepository`, con su orden de alta. El plan sugerido de una propuesta se guarda sin registrar: no se lista ni ocupa recursos hasta aceptarse, y al aceptarlo se registra la misma fila, como en memoria. Las altas llevan `registration_order` (identidad), que da el orden de alta de las páginas; `Page` cuenta con `COUNT`.

**Transacciones.** `TransactionalUseCase` decora cada interfaz de caso de uso con un proxy que ejecuta la operación en una transacción. `approve`, `accept` y `record` guardan dos agregados, y así lo hacen juntos. Es un decorador en `frameworks`: los interactors siguen sin anotaciones.

**Configuración.** `fieldops.persistence` elige `jpa` (por defecto) o `memory`, como implementaciones alternativas de los mismos puertos. El perfil `memory` además excluye datasource, JPA y Flyway. La conexión sale de `FIELDOPS_DB_URL`, `FIELDOPS_DB_USER` y `FIELDOPS_DB_PASSWORD`. `compose.yaml` levanta PostgreSQL local. `open-in-view` está apagado y las colecciones se leen por lotes.

**Tiempo.** PostgreSQL guarda microsegundos; `SystemClock` trunca a microsegundos para que un instante sellado por el sistema vuelva igual.

**Demostración.** `DemoData` (perfil `demo`) carga por los casos de uso, en una transacción y solo si no hay planes: un relevamiento aprobado con doce actividades de los seis tipos, un bloque secuencial con uno paralelo, personas y vehículos compartidos, la advertencia `CAPACITY` aceptada, la corrida iniciada y una propuesta pendiente por un incidente; y un plan nocturno en borrador sin permiso nocturno ni personal, con críticos. Las fechas salen del reloj, así la corrida y el incidente valen al cargar.

Descartado: guardar el plan como un documento JSON (pierde las claves foráneas y la integridad entre actividades); JDBC con SQL propio (la cursada usa JPA y Spring Data); anotar el dominio con JPA (los agregados son inmutables y validan al construirse); reconstruir por reflexión o repitiendo operaciones (saltea invariantes, no fija versión ni `supersedes` y depende del orden de alta); `@Transactional` en los interactors (metería Spring en `application`); H2 en los tests (no prueba la base que se usa).

Deuda: los ids de certificación y de consumible que pide una actividad no tienen clave foránea, porque `PlanItinerary` no los valida contra el catálogo y una clave rechazaría planes que hoy el dominio acepta. Las actividades de una corrida y de un incidente tampoco, porque pueden ser de una revisión. Al aceptarse, el plan sugerido pasa a ser el borrador que se edita y la propuesta lo ve cambiar. Un plan son unas dieciséis consultas, y `plans.all()` sigue cargando todos los planes. Un instante o una duración con más precisión que microsegundos que llega por la API se trunca. Un tipo nuevo de asignación toca el adaptador y el esquema. Los tests de persistencia necesitan Docker.

## Entrega 2



### Actividades nocturnas

Cada asignado necesita la certificación nocturna (`heldByEveryone`); la actividad pide iluminación (`InstrumentKind.LIGHTING`) y un permiso nocturno de la zona, y su riesgo sube un nivel (`RiskLevel.raised`). Estimación e informe leen el riesgo ya calculado. Un permiso de zona no reemplaza al nocturno.

Clases agregadas: `PermitKind`, `InstrumentKind.LIGHTING`. Modificadas: `Activity`, `ResourceRequirements`, `RiskLevel`, `Permit`, `AdministerPermits`, `AdministerPermitsInteractor`, `CertificationRule`, `MissingResourceRule`, `PermitRule`, `AssignmentSuggester`.

Refactorizaciones: builder por tipo en lugar de fábricas posicionales; iluminación y permiso nocturno como constantes del dominio; requisitos como conjuntos en lugar de `NONE | REQUIRED`; consumo estimado movido de los requisitos a la actividad; el sugeridor cubre `heldByEveryone`.

Descartado: subtipo de `Activity`, reabrir `ActivityPolicy`.

Deuda: el catálogo tiene que registrar la iluminación con `InstrumentKind.LIGHTING`.

### Bloques

El itinerario es un árbol de actividades y bloques. La secuencia suma y el paralelo toma el máximo, en la estimación y en la duración real; riesgo y consumo salen de las hojas. La secuencia ordena sus partes. `ParallelAssignmentRule` reserva cada rama sobre todo el período, por eso choca cualquier recurso compartido entre ramas. El informe cuenta hojas.

Clases agregadas: `ItineraryItem`, `ActivityBlock`, `ActivityBlock.Arrangement`, `Precedence`, `ParallelAssignmentRule`. Modificadas: `Activity`, `Itinerary`, `Expedition`, `Estimate`, `OperationalReport`, `PlanItinerary`, `PlanItineraryInteractor`, `TrackExpeditionInteractor`, `RuleBasedValidator`.

Refactorizaciones: `Arrangement` en lugar de booleano; una sola `duration(Function)` para la estimación y la duración real; secuencia como dependencias implícitas que reusan validación, `delay` y seguimiento; la regla paralela recorre `concurrentParts()` de cada bloque, sin preguntar el `Arrangement`; los `switch` de `Itinerary` pasaron a métodos de `ItineraryItem`; `Itinerary.copy` no copia nodos inmutables.

Descartado: aplanar para estimar (un paralelo contaría como secuencia); validar la secuencia en el constructor del bloque (`delay` reemplaza hoja por hoja); ordenar la raíz.

Deuda: la duración real usa el itinerario del plan pedido; una actividad que solo está en una revisión aprobada no suma.

### Replanificación por incidente

Un incidente sobre una actividad del plan vigente (`Revisions.inForce`) de una corrida genera una `ReplanProposal` sobre una revisión en borrador de ese plan, así que una replanificación aprobada no corta las propuestas siguientes: cancela si la actividad arrancó o si el retraso sale del período, retrasa si el incidente es posterior al inicio planificado y, si no, reemplaza recursos. La propuesta guarda el incidente, el plan sugerido y la decisión (quién y cuándo); el original se deriva del sugerido. Aceptar guarda el borrador y el original sigue aprobado hasta que se aprueba la revisión. Rechazar no toca nada. `ConsultExpedition` muestra el original y `ReviewReplanProposal.of` las propuestas.

Clases agregadas: `ProposalId`, `ReplanProposal`, `ReplanProposalRepository`, `ReviewReplanProposal`, `ReviewReplanProposalInteractor`, `RecordIncident`, `RecordIncidentInteractor`, `ConsultExpedition`, `ConsultExpeditionInteractor`, `PlanSnapshot`, `ProposalSnapshot`, `Revisions`. Modificadas: `Replanner`, `Expedition`, `ExpeditionExecution`, `Incident`, `ApproveExpeditionInteractor`, `ReplanExpeditionInteractor`, `TrackExpeditionInteractor`, `OccupyingExpeditions`.

Refactorizaciones: `ReplanProposer` pasó a `Replanner.respondTo`; la revisión toma el id del repositorio y rige al aprobarse; seguimiento e incidentes en casos de uso distintos; consultas con snapshots; se excluye todo el linaje y no solo el padre.

Descartado: aplicar el replan al registrar el incidente; bus de eventos; persistir el borrador antes de aceptar (se podría aprobar sin decisión); etiquetar el tipo de cambio; una strategy por acción; colgar la propuesta del plan o de la corrida; reemplazar al original al aceptar.

Deuda: las propuestas se consultan por el plan que revisan, así que después de una replanificación aprobada las nuevas aparecen en la revisión vigente y no en el original. En revisión, el original y sus revisiones ocupan frente a terceros. Una revisión aprobada de una raíz terminada sigue ocupando ventanas pasadas. `PlanningContexts` carga todos los planes y corridas (N+1 con persistencia real).