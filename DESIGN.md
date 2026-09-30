# Decisiones de diseño

## Aplicadas

**Arquitectura.** Clean Architecture empaquetada por componente. `catalog`, `expedition`, `itinerary`, `validation` y `report` tienen entidades, servicios, puertos e interactors; `tracking` tiene los valores del seguimiento (`ActivityExecution`, `Incident`, `Observation`); `identity`, `shared` y `assessment` son la base estable. La interfaz de cada caso de uso está en `<componente>.usecase` y el interactor junto a sus entidades, para usar las transiciones package-private (`markApproved`, `reviseAsDraft`, `submitForReview` y los mutadores de `ExpeditionExecution`): solo el caso de uso aprueba, revisa o registra la corrida. Por eso `ExpeditionExecution` y `ExecutionRepository` viven en `expedition`, junto a `TrackExpeditionInteractor` y `RecordIncidentInteractor`, aunque son un agregado aparte del plan. Dependencias sin ciclos: `validation → expedition → {tracking, itinerary, catalog, assessment}`, `report → {expedition, tracking}`, `tracking → {identity, shared}`. Los ids de planes, actividades, propuestas, certificaciones y recursos salen de los repositorios. El tiempo sale de `Clock`.

**Validador como puerto.** `ExpeditionValidator` es una interfaz de `expedition` y `RuleBasedValidator`, en `validation`, la implementa. Tiene una sola implementación porque existe para cortar el ciclo entre esos paquetes (DIP).

**Casos de uso.** `AdministerPersonnel`, `AdministerEquipment`, `AdministerPermits`, `DraftExpedition`, `PlanItinerary`, `AssignResources`, `ReviewExpedition`, `ApproveExpedition`, `TrackExpedition`, `RecordIncident`, `ReplanExpedition`, `ReviewReplanProposal`, `ConsultExpedition`, `EstimateExpedition`, `ReportExpedition`. Cada uno agrupa las operaciones de un actor; seguimiento e incidentes están separados porque cambian por motivos distintos. Los que modifican cargan por id, aplican y guardan; los de lectura no guardan. Ninguna entidad mutable sale: las consultas devuelven `PlanSnapshot`, `ProposalSnapshot`, `Estimate`, `OperationalReport` y `ValidationResult`, armados por el interactor. Entran ids y objetos inmutables.

**Puertos.** Lectura por recurso (`People`, `Vehicles`, `Instruments`, `Consumables`, `Permits`, `Certifications`), los reservables y de validación agrupados en `BookableResources` y `Catalogs`. La escritura también es por recurso: cada `*Registry` extiende su puerto de lectura con el próximo id y `save` (upsert), así la validación no depende de métodos de escritura. `ExpeditionRepository` (con `require` y los ids de actividad), `ExecutionRepository`, `ReplanProposalRepository`, `Clock`. Adaptadores en memoria y reloj fijo en `domain/src/test/java/edu/itba/fieldops/details`.

**Clean Code.** Hasta tres argumentos. Excepciones: records (`OperationalReport`, `PlanSnapshot`, `ExpeditionCharter`, `ResourceRequirements`, `Permit`), constructores de interactors (hasta seis colaboradores en `RecordIncidentInteractor`) y `Person`. `ExpeditionCharter` y `PlanningContext` agrupan argumentos que viajan juntos; `Activity` se arma con un builder por tipo. Sin banderas booleanas: `ActivityBlock.Arrangement` en vez de `boolean parallel`, `overlapsWithin` y `overlapsBetween` en vez de un `conflicts` con flag. Sin `null` en parámetros ni retornos: `Incident` guarda la actividad como `Optional` y `OperationalReport.of` tiene una versión con corrida y otra sin. CQS: `Assignments.withdraw` no devuelve nada; en `ReplanExpedition` solo `revise` crea y devuelve un id, y los cambios sobre el borrador no devuelven nada. Un término por concepto (`occupying`, `inForce`, `lineage`). Sin comentarios.

**Dos agregados.** `Expedition` es el plan (`DRAFT | IN_REVIEW | APPROVED | SUPERSEDED`) y `ExpeditionExecution` la corrida (`IN_PROGRESS | SUSPENDED | FINISHED`). `OperationalStatus` reúne los seis estados. Solo `DRAFT` se edita. Dentro del plan, `Expedition` tiene la identidad, la versión, el estado y las advertencias aceptadas, y `PlanContent` tiene charter, itinerario, asignaciones y permisos con las reglas que los cruzan (zona y período de cada actividad, asignación sobre una actividad existente, retraso dentro del período): cambian por motivos distintos, el flujo de revisión y la planificación. `Expedition` verifica el estado y delega; su API pública no expone `PlanContent`.

**Aprobación.** `ApproveExpedition` exige `IN_REVIEW`, revalida y rechaza con `ExpeditionNotApprovable` si hay críticos o advertencias sin aceptar. El agregado no recibe un `ValidationResult`. `submit` rechaza un itinerario vacío o con críticos. Una advertencia la acepta un responsable, con justificación, solo si la validación actual la produce; volver a borrador las borra.

**Revisiones.** Un plan aprobado no se edita. `reviseAsDraft(id)` crea una revisión (versión + 1, `supersedes`) con una copia del `PlanContent`, sin advertencias. La revisión rige cuando se aprueba: la aprobación marca `SUPERSEDED` al predecesor, que tiene que seguir `APPROVED`, y por eso se aprueba una sola revisión por plan. Hasta entonces el original sigue vigente y ocupando.

**Corrida y linaje.** La corrida vive en la raíz. `Revisions.inForce` pasa de un `SUPERSEDED` a su revisión aprobada: ese itinerario sigue el seguimiento. Ningún plan del linaje arranca otra corrida. Ocupan recursos los planes `IN_REVIEW` o `APPROVED` cuya corrida no terminó y los `SUPERSEDED` con corrida abierta. `OccupyingExpeditions` excluye el linaje del plan evaluado.

**Actividad.** Duración, riesgo y consumo estimado son datos. El tipo fija los requisitos (`ResourceRequirements`): certificaciones que alguien debe tener, las que debe tener cada asignado (`heldByEveryone`), tipos de instrumento, tipos de permiso especial (`PermitKind`) y cantidad de vehículos. Son conjuntos y cantidades, no banderas: las reglas y el sugeridor recorren lo pedido sin preguntar si hace falta. Seis tipos: muestreo, medición, traslado, nocturna, buceo, campamento. Un tipo nuevo es otra fábrica; reglas, estimación e informes no preguntan el tipo.

**Itinerario.** Composite: `ItineraryItem` es `Activity` o `ActivityBlock`. Es `sealed` porque hoja y compuesto es un conjunto cerrado; cada variante resuelve `blocks`, `precedence`, `replacing` y `without`, y no hay `switch` sobre el tipo. `Arrangement` decide qué agrega el orden (`order`) y qué partes corren a la vez (`concurrent`). `duration(Function)` suma en la secuencia y toma el máximo en paralelo. La raíz suma sin ordenar. En un bloque secuencial cada parte depende de todas las hojas de la anterior; esas dependencias se suman a las explícitas para validar ventanas y ciclos, para `delay` y para el seguimiento.

**Asignaciones.** `Assignments` guarda una lista por tipo y cada asignación se archiva sola (double dispatch), sin `instanceof`. `BookableAssignment` (persona, vehículo, instrumento) da el id del recurso (`BookableId`) y lo busca en el catálogo (`Bookable`); con eso `TemporalBooking` es un solo record que compara ids y ventanas y pregunta la disponibilidad, sin métodos por tipo. El consumible solo declara una cantidad positiva. `Expedition.assignments()` devuelve una copia.

**Catálogo.** Tres casos de uso, uno por actor: `AdministerPersonnel` da de alta certificaciones y personas, certifica con certificaciones registradas y cambia la disponibilidad de las personas; `AdministerEquipment` da de alta vehículos, instrumentos y consumibles, cambia su disponibilidad y el stock; `AdministerPermits` da de alta permisos. Entidades inmutables (`withAvailability`, `certified`, `withStock`). El permiso tiene un `PermitKind` (`ZONE`, `NIGHT`) y cubre zona y vigencia; un tipo de permiso nuevo es otra constante.

**Validación.** Strategy: `RuleBasedValidator` recorre las reglas que recibe y `withDefaultRules` arma las siete. Otra regla es otra clase (`ValidationExtensionTest`). Todas reciben `PlanningContext`.

**Códigos.** `RESOURCE`: id desconocido (asignación o permiso) o actividad sin la persona, el vehículo o el instrumento que pide. `CERTIFICATION`: ninguna persona conocida tiene la certificación pedida a alguien, o alguna no tiene la pedida a todos. `PERMIT`: actividad sin un permiso conocido que la cubra, o sin un permiso de cada tipo especial que pide; sin adjuntos quedan todas descubiertas, y si todos los adjuntos son desconocidos solo sale `RESOURCE`. `OVERLAP`: recurso en ventanas superpuestas, propias o de las que ocupan. `AVAILABILITY`: recurso conocido fuera de su disponibilidad. `PARALLEL`: recurso en dos ramas de un bloque paralelo, con cualquier ventana. `STOCK`: lo asignado, propio y de las que ocupan, supera el depósito. `CAPACITY` es `WARNING`; el resto, `CRITICAL`. Un id ya en `RESOURCE` no se evalúa por disponibilidad, stock, certificación ni capacidad. Los códigos son `String` para que una regla nueva no toque un enum.

**Sugerencias.** `AssignmentSuggester` cubre vehículo, instrumento del tipo pedido y personas con lo que se exige a todos. Elige el primer recurso disponible y libre frente a las reservas propias y de las que ocupan. No modifica el plan.

**Replan.** `ReplanExpedition.revise` crea la revisión en borrador de un plan `APPROVED`; `cancel`, `delay` y `replaceUnavailable` solo cambian borradores, así que un plan `IN_REVIEW` vuelve primero a borrador con `ReviewExpedition.returnToDraft`. Cada operación tiene un solo efecto y el id del plan no cambia según el estado. `cancel` quita la actividad y las dependencias hacia ella. `delay` corre la actividad y empuja a sus dependientes; se rechaza si alguna ventana sale del período. `replaceUnavailable` suelta reservas no disponibles o en conflicto. Las tres rellenan con el sugeridor. `respondTo` elige la respuesta a un incidente.

**Seguimiento.** Una actividad arranca dentro de su ventana y con sus predecesores, explícitos y de secuencia, terminados. Se cierra después de su inicio aunque pase la ventana. La expedición termina cuando está terminado lo iniciado y lo del plan vigente. Incidentes y observaciones se registran con la corrida en curso o suspendida. `Clock` sella los instantes.

**Estimación e informe.** `Estimate`: duración del árbol, riesgo máximo (`LOW` sin actividades) y consumo estimado de las actividades. `OperationalReport` sin corrida informa la duración estimada y el consumo asignado; con corrida, la duración medida combinada en el árbol, el consumo asignado a lo terminado, los incidentes, las observaciones y los resultados.

**Invariantes.** El charter exige objetivos, zonas y responsables, y los responsables tienen que existir en el catálogo. La ventana alcanza la duración estimada. Los predecesores existen, no forman ciclos y terminan antes. La zona es de la expedición y la ventana está dentro del período. `TimePeriod`, `Stock`, `Passengers`, `WorkZone` y `ExpeditionCharter` se validan al construirse.

**Errores.** Todo extiende `DomainException`. `InvalidValue`: dato inválido, id desconocido, no responsable, advertencia inexistente, duplicado o propuesta ya decidida. `InvalidItinerary`: grafo, ventanas y bloques. `InvalidAssignment`: asignación repetida, desconocida o sin cantidad. `InvalidActivityExecution`: seguimiento ilegal. `InvalidExpeditionTransition`: transición de estado ilegal del plan o de la corrida. `ExpeditionNotApprovable`: críticos o advertencias sin justificar. Un nulo en un constructor o un paso faltante del builder lanza `NullPointerException`.

**Tests.** Unitarios por componente, cada uno con lo mínimo que la regla necesita: el seguimiento se prueba sin armar un plan y el informe sin aprobarlo. `ExpeditionEditing` y `ExecutionEditing` exponen las operaciones package-private a los tests de otros paquetes. Las reglas del contenido se prueban en `PlanContentTest` y las del estado y las revisiones en `ExpeditionTest`. Los casos de uso se prueban por grupo (`CatalogUseCasesTest`, `PlanningUseCasesTest`, `ReviewUseCasesTest`, `TrackingUseCasesTest`, `ReplanningUseCasesTest`) sobre `UseCaseFixture`, que arma los interactors con los adaptadores en memoria y los escenarios comunes. La aprobación se prueba solo con los casos de uso, porque la ejecuta el interactor.

## Descartadas

**Política por tipo o herencia de `Activity`.** Los tipos difieren en datos. Un tipo nuevo es una fábrica.

**Supertipo de todos los ids o de las cantidades.** Mezclaría stock con pasajeros. Solo los ids reservables comparten `BookableId`, que no confunde una persona con un vehículo porque la igualdad es por tipo. El costo son métodos paralelos por tipo en `Assignments`.

**State por estado.** `ExpeditionStatus` solo responde `occupiesResources` y las transiciones preguntan la constante. Un estado nuevo toca las transiciones del agregado.

**Aprobar un `ValidationResult` ya calculado.** Puede no describir el plan actual.

**Versionar el borrador.** `DRAFT` e `IN_REVIEW` se editan en el lugar.

**Reordenar una lista.** El orden sale de dependencias, ventanas y bloques secuenciales.

**`switch` sobre un `Assignment` sealed.** Lo reemplaza el double dispatch.

**Un interactor por operación y request models.** Se agrupa por actor y no sale ninguna entidad mutable; las entradas usan objetos de dominio inmutables.

**Módulo Maven de detalles.** No hay otro runtime; los adaptadores viven en test.

**Códigos de issue como enum.** Cada regla nueva tocaría el enum; se escriben como `String`.

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

Un incidente sobre una actividad de un plan `APPROVED` con corrida genera una `ReplanProposal` sobre una revisión en borrador: cancela si la actividad arrancó o si el retraso sale del período, retrasa si el incidente es posterior al inicio planificado y, si no, reemplaza recursos. La propuesta guarda el incidente, el plan sugerido y la decisión (quién y cuándo); el original se deriva del sugerido. Aceptar guarda el borrador y el original sigue aprobado hasta que se aprueba la revisión. Rechazar no toca nada. `ConsultExpedition` muestra el original y `ReviewReplanProposal.of` las propuestas.

Clases agregadas: `ProposalId`, `ReplanProposal`, `ReplanProposalRepository`, `ReviewReplanProposal`, `ReviewReplanProposalInteractor`, `RecordIncident`, `RecordIncidentInteractor`, `ConsultExpedition`, `ConsultExpeditionInteractor`, `PlanSnapshot`, `ProposalSnapshot`, `Revisions`. Modificadas: `Replanner`, `Expedition`, `ExpeditionExecution`, `Incident`, `ApproveExpeditionInteractor`, `ReplanExpeditionInteractor`, `TrackExpeditionInteractor`, `OccupyingExpeditions`.

Refactorizaciones: `ReplanProposer` pasó a `Replanner.respondTo`; la revisión toma el id del repositorio y rige al aprobarse; seguimiento e incidentes en casos de uso distintos; consultas con snapshots; se excluye todo el linaje y no solo el padre.

Descartado: aplicar el replan al registrar el incidente; bus de eventos; persistir el borrador antes de aceptar (se podría aprobar sin decisión); etiquetar el tipo de cambio; una strategy por acción; colgar la propuesta del plan o de la corrida; reemplazar al original al aceptar.

Deuda: un incidente sobre un plan reemplazado con corrida abierta no propone, porque las propuestas se consultan por el id del plan que revisan. En revisión, el original y sus revisiones ocupan frente a terceros. Una revisión aprobada de una raíz terminada sigue ocupando ventanas pasadas. `PlanningContexts` carga todos los planes y corridas (N+1 con persistencia real).
