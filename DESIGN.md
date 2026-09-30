# Decisiones de diseño

## Aplicadas

**Dos agregados.** `Expedition` es el plan (`DRAFT | IN_REVIEW | APPROVED | SUPERSEDED`). `ExpeditionExecution` es la corrida (`IN_PROGRESS | SUSPENDED | FINISHED`). Itinerario, validador, sugeridor, replanner e informe quedan afuera. El caso de uso cruza plan y corrida.

**Casos de uso.** Cada flujo es una interfaz en `<subdominio>.usecase` y un interactor en el paquete del subdominio. El interactor no vive en `usecase`: el de plan alcanza las transiciones de paquete. El que modifica carga por id, aplica y guarda. El de lectura no guarda. Ninguno recibe el plan ni la corrida ya armados. `AdministerCatalog` vive en `catalog.usecase`. `EstimateExpedition` y `ReportExpedition` en `report.usecase`. El resto en `expedition.usecase`. `ExpeditionValidator`, `AssignmentSuggester` y `Replanner` son servicios, no el camino de la aplicación. Transiciones del plan y `Expedition.draft` son de paquete. Volver a borrador es de `ReviewExpedition`: solo desde `IN_REVIEW`, sin tocar la corrida. Las de la corrida son públicas: `TrackExpedition` en `expedition.usecase`, `ExpeditionExecution` en `tracking`.

**Puertos.** `People`, `Vehicles`, `Instruments`, `Consumables` y `Permits` consultan cada recurso. `BookableResources` junta los tres con ventana. `Catalogs` suma consumibles y permisos. `CatalogRegistry` da de alta persona, vehículo, instrumento, consumible y permiso; la certificación va con la persona. `ExpeditionRepository` y `ExecutionRepository` persisten. `Clock.now()` sella inicio y fin de actividad, incidentes y observaciones; en el arranque de la expedición solo chequea el período. Adaptador, catálogo y reloj fijo están en `domain/src/test/java/edu/itba/fieldops/details`. `src/main` no los importa.

**Aprobación.** `ApproveExpedition` exige `IN_REVIEW`, revalida y llama a `markApproved`. El agregado no recibe un `ValidationResult`. Ese resultado guarda id y versión del plan validado. Críticos o warning sin justificar: `ExpeditionNotApprovable`. Solo `DRAFT` se edita. `IN_REVIEW` ocupa y puede volver a borrador. Una advertencia la acepta un responsable. `submit` rechaza críticos. Las restricciones son texto.

**Identidad y cantidades.** Cada id es un record sobre un UUID, en `identity`. La reserva es `PersonBooking`, `VehicleBooking` o `InstrumentBooking`: el id no se aplana. `Stock` y `Passengers` no comparten supertipo. Cero es válido.

**Actividad.** Duración, riesgo, requisitos y consumo estimado son datos. Vehículo: `NONE | REQUIRED`. Instrumento: `None | OfKind`. La asignación implementa `booking`, `unknownIn` y se archiva sola. Solo el consumible declara cantidad; `Assignments.consumption()` la suma. Una reserva nueva responde `conflictsWith` para su id. Asignación por actividad. Persona, vehículo e instrumento se reusan con ventanas disjuntas. El consumible, por stock del depósito.

**Permiso y certificación.** `Permit` cubre zona y vigencia, adjunto a la expedición. `Certification` vive en la persona y la pide el requisito. Vehículos e instrumentos, por disponibilidad.

**Catálogo y reserva.** Ocupa `IN_REVIEW` o `APPROVED` cuya ejecución no está `FINISHED`. Un `SUPERSEDED` ocupa a las demás si su corrida sigue. `OccupyingExpeditions` excluye al propio plan, al que esta revisión supersede, y a una terminada si el llamador pasa su ejecución. El sucesor no compite con el original: copia itinerario y asignaciones. El stock del catálogo es el depósito: lo asignado en el plan y en las que ocupan se compara con ese depósito.

**Assessment.** `ValidationResult`, `ValidationIssue` e `IssueSeverity` viven fuera de `validation` y de `expedition` para no ciclar paquetes. `ValidationContext` lleva expedición, catálogos y las que ocupan. `ExpeditionValidator` recorre las reglas del constructor; `withDefaultRules` arma las siete. Otra regla es otra clase.

**Códigos.** `RESOURCE`: id ausente (asignación o permiso) o actividad que pide persona certificada, vehículo o instrumento y no lo tiene. `CERTIFICATION`: personas conocidas asignadas y ninguna con la certificación pedida a alguien, o alguna sin la que el requisito pide a todos. `PERMIT`: actividad no cubierta por los permisos conocidos, o sin permiso nocturno si lo pide. Adjuntos vacíos = todas descubiertas. Adjuntos todos desconocidos = solo `RESOURCE`. Un id ya en `RESOURCE` omite disponibilidad, stock, certificación y capacidad. `OVERLAP` y `AVAILABILITY` salen de `TemporalOverlapRule`. `PARALLEL`: persona, vehículo o instrumento en dos ramas de un bloque paralelo; las ventanas disjuntas no alcanzan. El consumible no entra. `STOCK` compara lo asignado (propio y ocupantes) con el depósito. `CAPACITY` es `WARNING`; el resto, `CRITICAL`. Capacidad = vehículos de esa actividad. Pasajeros = personas de la misma.

**AssignmentSuggester.** Huecos de certificación, vehículo e instrumento. Primer recurso del catálogo disponible y libre frente a asignaciones propias y ocupantes. No toca el plan; `addAssignment` aplica.

**TemporalBooking.** Persona, vehículo o instrumento en una ventana. Lo usan `TemporalOverlapRule`, `AssignmentSuggester` y `Replanner`. Ausente no está `availableIn`. `unavailableIn` es recurso conocido y no listo.

**Replan.** `Replanner` edita el plan que recibe. `IN_REVIEW` vuelve a `DRAFT` antes. Desde `APPROVED`, el caso de uso llama a `reviseAsDraft` (id nuevo, versión + 1, `supersedes`, sin corrida ni warnings) y le pasa esa copia, también con corrida abierta. Copia objetivos, período, zonas, responsables, restricciones, itinerario, asignaciones y permisos. No copia ejecuciones, incidentes, observaciones ni warnings. Marca el original `SUPERSEDED` y guarda los dos. No lo reenvía ni lo aprueba. Cancelar un predecesor suelta esa dependencia. `delay` corre la actividad y empuja dependientes; si alguna ventana se sale del período, se rechaza. Suelta persona, vehículo o instrumento inválido por catálogo o solape; el consumible queda. Después rellena con el sugeridor. La corrida queda en el original. El seguimiento usa el itinerario de la última revisión: no arranca una cancelada, el retraso vale para lo que falta, `finish` exige lo de esa revisión más lo ya iniciado. Una actividad ya iniciada se cierra aunque la revisión la haya cancelado. La revisión no arranca otra corrida hasta que termine la original. `ActivityExecution.finish` devuelve otra instancia. Suspender solo desde `IN_PROGRESS`. Incidentes y observaciones en `IN_PROGRESS` y `SUSPENDED`; el incidente puede nombrar una actividad.

**Informe.** Estado, planificadas, iniciadas, terminadas, incidentes y resultados cerrados. Sin corrida, duración y consumo son los del plan. Con actividades terminadas, duración = `finishedAt - startedAt` y consumo = lo asignado a esas. Lo no terminado no cuenta. `Estimate.of` arma duración, riesgo (máximo del itinerario, `LOW` si vacío) y consumo de los requisitos, no de lo asignado. El estado operativo es el de la corrida si existe; si no, el del plan (`SUPERSEDED` incluido). Borrador, revisión y aprobada en el plan; en ejecución, suspendida y finalizada en la corrida.

**Invariantes.** La ventana alcanza la duración estimada. Predecesores existen, acíclicos y terminan antes del inicio, en el plan y al ejecutar. La zona de la actividad está en la expedición. Itinerario no vacío al enviar a revisión. `TimePeriod`, `Stock`, `Passengers` y `WorkZone` se validan al construirse. Inicio de expedición y de cada actividad en el período; la actividad, en su ventana.

**Servicios.** `ExpeditionValidator` recibe las reglas. `Replanner` recibe el sugeridor. `AssignmentSuggester` no tiene estado.

**Errores.** Todo extiende `DomainException`. `InvalidExpeditionTransition` está en `shared` para que `tracking` no dependa de `expedition`. `ExpeditionNotApprovable` es aprobación bloqueada. `InvalidActivityExecution` es seguimiento ilegal. `InvalidValue` es dato o id inválido. `InvalidItinerary` es el grafo y las ventanas. Nulo en constructor: `NullPointerException`. El servicio no revalida que expedición o catálogo existan.

## Descartadas

**Política por tipo de actividad.** Duración y riesgo varían entre dos muestreos. Un tipo nuevo es otra fábrica.

**Heredar `Activity`, flota en la expedición, supertipo de recurso o de cantidad, `Id<T>`.** Mezclan stock con pasajeros, o un id de persona con uno de vehículo.

**State por estado.** No hay un objeto por estado. `IN_REVIEW` y `APPROVED` ocupan; el resto pregunta la constante.

**Aprobar un `ValidationResult` ya calculado.** Puede no describir el plan actual.

**Versionar también el borrador.** En `DRAFT` e `IN_REVIEW` se edita en el lugar.

**Reordenar el itinerario.** El orden de la lista no es el del negocio. Lo son las dependencias y las ventanas.

**Módulo Maven de detalles.** El dominio no tiene otro runtime. El adaptador vive en el source de test.

## Entrega 2

### Actividad nocturna

`Activity.night` guarda el riesgo ya subido un nivel (`LOW` pasa a `MEDIUM`, `MEDIUM` y `HIGH` quedan en `HIGH`) y los requisitos. `Estimate` y `OperationalReport` no distinguen el tipo: leen duración, riesgo y consumo.

`ResourceRequirements.heldByEveryone` es la certificación que tiene que tener cada persona asignada. El conjunto `certifications` sigue siendo “alguien la tiene”. `NightPermit` es `NONE | REQUIRED`, igual que el vehículo. `Permit.Kind` separa el permiso de zona del nocturno. `registerPermit` da de alta el de zona; `registerNightPermit`, el nocturno. `PermitRule` exige el nocturno solo si la actividad lo pide. Un permiso de zona de la misma zona no alcanza. La iluminación es `InstrumentRequirement.OfKind`.

Clases agregadas: `NightPermit`. Modificadas: `Activity`, `ResourceRequirements`, `RiskLevel`, `Permit`, `AdministerCatalog`, `AdministerCatalogInteractor`, `CertificationRule`, `MissingResourceRule`, `PermitRule`.

Descartado: un subtipo de `Activity` y reabrir `ActivityPolicy`. El feedback ya sacó la duración y el riesgo de la policy.

Deuda: `AssignmentSuggester` no completa una dotación para `heldByEveryone`. La validación lo exige; la sugerencia sigue cubriendo el hueco de “alguien”.

### Bloques

El itinerario es un árbol. La raíz es una secuencia. Un nodo es una actividad o un `ActivityBlock` secuencial o paralelo. Solo la duración usa el árbol: secuencial suma, paralelo toma el máximo. Riesgo y consumo salen de las hojas. `Estimate` le pide esos tres al itinerario. El informe cuenta hojas: un bloque no se inicia ni se termina. `delay` y `copy` recorren el árbol. `PlanItinerary.addBlock` da de alta el bloque; cancelar una hoja lo saca y, si queda una sola parte, el bloque se aplana a esa parte. El grafo de predecesores sigue siendo el de las actividades.

`ParallelAssignmentRule` recorre cada bloque paralelo. Persona, vehículo o instrumento en dos ramas: `PARALLEL`. Un bloque no tiene predecesores propios.

Clases agregadas: `ItineraryItem`, `ActivityBlock`, `ParallelAssignmentRule`. Modificadas: `Activity`, `Itinerary`, `Expedition`, `Estimate`, `PlanItinerary`, `PlanItineraryInteractor`, `ExpeditionValidator`.

Descartado: aplanar el árbol para estimar. La suma de hojas trata un paralelo como secuencia.
