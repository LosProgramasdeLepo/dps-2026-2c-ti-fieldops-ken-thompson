# Decisiones de diseño

## Aplicadas

**Dos agregados.** `Expedition` es el plan (`DRAFT | IN_REVIEW | APPROVED | SUPERSEDED`). `ExpeditionExecution` es la corrida (`IN_PROGRESS | SUSPENDED | FINISHED`). Itinerario, validador, sugeridor, replanner e informe quedan afuera del agregado. El caso de uso es el que cruza plan y corrida.

**Casos de uso.** Cada flujo de la consigna es una interfaz y un interactor. El que modifica carga por id, aplica la regla y guarda. El de lectura no guarda. Ninguno recibe el plan ni la corrida ya armados. `AdministerCatalog` vive en `catalog`. `EstimateExpedition` y `ReportExpedition` viven en `report`. El resto vive en `expedition`, para alcanzar las transiciones de paquete. `ExpeditionValidator`, `AssignmentSuggester` y `Replanner` siguen siendo servicios y dejan de ser el camino de la aplicación. Las transiciones del plan, y `Expedition.draft`, son de paquete. El caso de uso pide el id al repositorio y llama a esa fábrica. Volver a borrador es de `ReviewExpedition`: solo sale de `IN_REVIEW` y no toca la corrida. Las de la corrida siguen públicas: `TrackExpedition` vive en `expedition` y `ExpeditionExecution` en `tracking`.

**Puertos.** `People`, `Vehicles`, `Instruments`, `Consumables` y `Permits` consultan cada recurso. `BookableResources` junta los tres que ocupan una ventana. `Catalogs` agrega consumibles y permisos cuando la operación los necesita juntos. `CatalogRegistry` da de alta persona, vehículo, instrumento, consumible y permiso; la certificación viaja con la persona. `ExpeditionRepository` y `ExecutionRepository` persisten cada agregado. `Clock.now()` es el instante de inicio y fin de actividad. El adaptador en memoria, el catálogo concreto y el reloj fijo están en `domain/src/test/java/edu/itba/fieldops/details`. `src/main` no los importa.

**Aprobación.** `ApproveExpedition` exige `IN_REVIEW`, revalida el plan actual y llama a `markApproved`, de paquete. El agregado no recibe un `ValidationResult`. `ValidationResult` guarda el id y la versión del plan que se validó. Solo `DRAFT` se edita: `IN_REVIEW` ocupa y puede volver a borrador, pero no se modifica. Una advertencia la acepta un responsable.

**Identidad y cantidades.** Cada id es un record distinto sobre un UUID, en `identity`, para que un `PersonId` no entre donde se espera un `VehicleId`. La reserva es `PersonBooking`, `VehicleBooking` o `InstrumentBooking`: el id no se aplana. `Stock` y `Passengers` no comparten supertipo. Cero es válido.

**Actividad.** Duración, riesgo, requisitos y consumo estimado son datos. Las fábricas arman el requisito de vehículo (`NONE | REQUIRED`) e instrumento (`None | OfKind`). Una asignación implementa `booking`, `unknownIn` y se archiva sola en su lista: no hay `switch` ni `instanceof` por tipo. Una reserva nueva responde `conflictsWith` para su id; las que ya existen no se tocan. `None` no pide clase y `OfKind` expone `requiredKind`.

**Catálogo y reserva.** Ocupa un plan `IN_REVIEW` o `APPROVED` cuya ejecución no está `FINISHED`. Un `SUPERSEDED` ocupa solo si su corrida sigue en curso o suspendida. `OccupyingExpeditions` excluye al propio plan, al que esta revisión supersede cuando ese plan ya no reserva, y a una terminada solo si el llamador pasa su ejecución.

**Assessment.** `ValidationResult` vive fuera de `validation` y de `expedition`. Otra regla es otra clase en la lista del validador. `CAPACITY` es `WARNING`; el resto de los códigos es `CRITICAL`.

**Replan.** `Replanner` siempre edita el plan que recibe. `IN_REVIEW` vuelve a `DRAFT` antes de tocarlo. Desde `APPROVED`, el caso de uso llama a `reviseAsDraft` (id nuevo, versión + 1, `supersedes`, sin corrida ni warnings) y le pasa esa copia. Después marca el original `SUPERSEDED` y guarda los dos. No lo reenvía ni lo aprueba. `ActivityExecution.finish` devuelve otra instancia. Suspender solo sale de una corrida `IN_PROGRESS`.

**Informe.** Sin corrida, duración y consumo son los del plan. Con actividades terminadas, la duración suma `finishedAt - startedAt` y el consumo suma lo asignado a esas actividades. Lo no terminado no cuenta como consumido. La estimación usa el consumo de los requisitos, no la cantidad asignada. El estado operativo es el de la corrida si existe (`IN_PROGRESS | SUSPENDED | FINISHED`); si no, el del plan. Los seis estados de la consigna quedan así: el plan tiene borrador, revisión y aprobada; la corrida tiene en ejecución, suspendida y finalizada.

**Errores.** Todo extiende `DomainException`. `InvalidExpeditionTransition` está en `shared` para que `tracking` no dependa de `expedition`. Un nulo no es un valor del dominio: lo rechaza el constructor del modelo, con `NullPointerException`. El servicio no vuelve a validar que la expedición o el catálogo existan.

## Descartadas

**Política por tipo de actividad.** Duración y riesgo varían entre dos muestreos. Un tipo nuevo es otra fábrica.

**Heredar `Activity`, flota en la expedición, supertipo de recurso o de cantidad, `Id<T>`.** Mezclan stock con pasajeros, o un id de persona con uno de vehículo.

**State por estado.** No hay un objeto por estado. Cada constante de `ExpeditionStatus` declara qué permite.

**Aprobar un `ValidationResult` ya calculado.** Puede no describir el plan actual.

**Versionar también el borrador.** Antes de aprobar el plan todavía se está armando.

**Reordenar el itinerario.** El orden de la lista no es el del negocio. Lo son las dependencias y las ventanas.

**Módulo Maven de detalles.** El dominio no tiene otro runtime, y un módulo que el dominio necesitara en los tests cerraría un ciclo. El adaptador vive en el source de test.
