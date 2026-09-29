# Decisiones de diseño

## Aplicadas

**Dos agregados.** `Expedition` es el plan (`DRAFT | IN_REVIEW | APPROVED`). `ExpeditionExecution` es la corrida (`IN_PROGRESS | SUSPENDED | FINISHED`). `ExpeditionLifecycle` cruza ambos y exige que la ejecución sea de ese plan. Itinerario, validador, sugeridor, replanner e informe quedan afuera del agregado.

**Aprobación.** El único camino público es `ApproveExpedition`: exige `IN_REVIEW`, revalida el estado actual y llama a `markApproved`, de paquete. El agregado no recibe un `ValidationResult` ni importa al validador. `ApproveExpedition` vive en `expedition` para poder llamar a `markApproved`; por eso `expedition` y `validation` se referencian.

**Identidad y cantidades.** Cada id es un record distinto sobre un UUID, en `identity`, para que un `PersonId` no entre donde se espera un `VehicleId`. La reserva es `PersonBooking`, `VehicleBooking` o `InstrumentBooking`: el id no se aplana. `Stock` y `Passengers` no comparten supertipo. Cero es válido.

**Actividad.** Duración, riesgo, requisitos y consumo estimado son datos. Las fábricas arman el requisito de vehículo (`NONE | REQUIRED`) e instrumento (`None | OfKind`). Una asignación nueva implementa `booking` y `unknownIn`; `None` no pide clase y `OfKind` expone `requiredKind`.

**Catálogo y reserva.** `Catalog` es el puerto de consulta. `ResourceCatalog` guarda las altas. Ocupa un plan `IN_REVIEW` o `APPROVED` cuya ejecución no está `FINISHED`. `OccupyingExpeditions` excluye al propio plan, al que esta revisión supersede, y a una terminada solo si el llamador pasa su ejecución.

**Assessment.** `ValidationResult` vive fuera de `validation` y de `expedition`. Otra regla es otra clase en la lista del validador. `CAPACITY` es `WARNING`; el resto de los códigos es `CRITICAL`.

**Replan.** Antes de aprobar se edita en el lugar. Desde `APPROVED`, `reviseAsDraft` copia solo el plan (id nuevo, versión + 1, `supersedes`) y no toca la corrida ni los warnings. `ActivityExecution.finish` devuelve otra instancia.

**Errores.** Todo extiende `DomainException`. `InvalidExpeditionTransition` está en `shared` para que `tracking` no dependa de `expedition`. `requireNonNull` sigue siendo `NullPointerException`.

## Descartadas

**Política por tipo de actividad.** Duración y riesgo varían entre dos muestreos. Un tipo nuevo es otra fábrica.

**Heredar `Activity`, flota en la expedición, supertipo de recurso o de cantidad, `Id<T>`.** Mezclan stock con pasajeros, o un id de persona con uno de vehículo.

**State por estado.** El conjunto es cerrado: un valor del enum y una guarda.

**Aprobar un `ValidationResult` ya calculado.** Puede no describir el plan actual.

**Versionar también el borrador.** Antes de aprobar el plan todavía se está armando.
