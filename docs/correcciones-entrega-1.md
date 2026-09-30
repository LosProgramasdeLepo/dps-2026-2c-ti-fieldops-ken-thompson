# Correcciones

## 1. Modelado del dominio

### Bien

Modelan las partes difíciles del enunciado: grafo de dependencias acíclico, ventanas dentro del período, reserva de recursos entre expediciones (`TemporalBooking`, `occupyingPeers`) y replanificación. Si el plan ya estaba aprobado, se genera una versión nueva (`reviseAsDraft`, con `supersedes`) y la aprobada no se modifica.

`ActivityExecution` es inmutable, e `Itinerary.delayed` calcula el resultado sin modificar nada para validar antes de aplicar.

El comportamiento que cruza la expedición con el catálogo está en servicios del dominio (`ExpeditionValidator`, `AssignmentSuggester`, `Replanner`) y no en el agregado.

### Falta

La regla central del enunciado ("no aprobar con errores críticos") no la protege el agregado, porque confía en un resultado que le pasa quien llama:

```java
public void approve(ValidationResult validation) {
    requireStatus(ExpeditionStatus.IN_REVIEW, "approve");
    requireApprovable(validation);   // solo mira validation.hasCritical()
    status = ExpeditionStatus.APPROVED;
}
```

`e.approve(ValidationResult.empty())` aprueba una expedición con críticos. Lo mismo pasa con un resultado viejo: validar, agregar una asignación inválida en `IN_REVIEW` y aprobar. El agregado tiene que ejecutar la validación (por ejemplo, recibir una `ApprovalPolicy`) o el único camino tiene que ser un caso de uso.

`Expedition` es un modelo mutable de 424 líneas y 16 campos que mezcla plan, asignaciones, advertencias, seguimiento y versionado. Plan y ejecución deberían ser agregados separados, y el comportamiento que hoy tiene el agregado podría vivir en servicios del dominio, dejando al modelo la responsabilidad de representar.

Ids crudos: todo es `UUID` (persona, vehículo, permiso, certificación, responsable). `new PersonAssignment(activityId, vehicle.id())` compila. Faltan `PersonId`, `VehicleId`, etc.

Primitive obsession: `ResourceRequirements(Set<UUID> certifications, boolean needsVehicle, boolean needsInstrument)` usa flags en lugar de conceptos (una medición exige "algún instrumento", pero no dice cuál). `Instrument.kind` es un `String` que nadie consulta.

`Quantity(int value)` es un value object que se utiliza para propósitos diferentes. Está bien la idea de limitar valores con este modelado; de hecho está bueno que en el constructor fuercen que el valor sea mayor a cero. El problema está en que este modelo cumple demasiados roles:

- En `Consumable.java`: `private final Quantity stock;` define la cantidad de un stock (podría ser simplemente `Stock`).
- En `CapacityRules.java`: `if (capacity.isAtLeast(new Quantity(passengers)))` — `passengers` podría ser de tipo `Passengers`.

Es importante esta distinción porque, si bien en ambos casos pueden compartir la validación de `isAtLeast()` que provee `Quantity`, puede haber otras validaciones que no compartan. Por ejemplo, stock pueden ser unidades, litros, gramos, mientras que passengers pueden ser unidades, pero también pueden estar catalogados por altura de pasajeros en el futuro, para brindar un mejor servicio, o cualquier otra validación. En definitiva se están representando dos conceptos diferentes con un mismo modelo, cuando lo que se busca es ser lo más fiel al dominio posible, para que la aplicación sea lo más robusta posible y resiliente ante cambios del negocio.

Lo mismo con las colecciones: `List<Expedition> others`, `List<Assignment>`, `List<AcceptedWarning>` podrían ser aggregates propios (`Assignments`, `OccupyingExpeditions`…) que además de aportar lenguaje ubicuo permitan poner restricciones sobre esas colecciones.

Las políticas tienen duración y riesgo fijos en el código (`SamplingPolicy` devuelve siempre 4 h / `MEDIUM`), y el consumo estimado no está modelado.

`ActivityPolicies` es un nombre complicado. En la jerga del desarrollo de software la palabra *policy* tiene un significado muy concreto: una serie de reglas que se aplican y generan resultados (patrón policy). En el caso de esta app, las policies son un catálogo de características estático.

Además en algunos casos tienen responsabilidades que no deberían caer en una policy. Por ejemplo, en `Activity.java`:

```java
public final class Activity {
    // ...
    public Duration estimatedDuration() { return policy.estimatedDuration(); }
    // ...
}
```

La duración de una actividad debería conocerla la misma actividad; no es responsabilidad de una política saber eso. De hecho el funcionamiento es al contrario: `Activity` debería conocer sus datos y luego las policies deberían aplicarse mediante polimorfismo basado en los datos de la activity. Actualmente el polimorfismo es casi un enum.

Casi todas las reglas tiran `IllegalArgumentException`. Hay 3 excepciones propias, sin una jerarquía de dominio.

## 1.2 Casos de uso

No hay casos de uso: quien llama (en la práctica, los tests) usa directamente `Expedition`, `ExpeditionValidator`, `AssignmentSuggester`, `Replanner` y `OperationalReport`. Todo lo que hace ese código a mano (conseguir el catálogo, conseguir "las otras expediciones", guardar, validar antes de aprobar) es trabajo que tendría que coordinar un caso de uso.

La responsabilidad de orquestar las entidades y servicios del dominio para modelar el flujo de la aplicación según lo que requiere el negocio es totalmente del dominio, porque es una característica core o central. El "qué" del flujo, la descripción de cómo opera la aplicación, es responsabilidad del dominio. El "cómo" — la forma en la que se logra en bajo nivel, los accesos a persistencia, llamados a APIs, publicación de eventos — es responsabilidad de los detalles.

Del mismo modo, declarar "qué" se hace incluye la declaración de puertos con el exterior, ya que el negocio quizás no conoce si tendrá que realizar un llamado a una base de datos SQL o un llamado a una API HTTP. Lo que sí conoce el dominio es dónde termina su responsabilidad, y esa barrera debe estar definida dentro del dominio, como se vio en la clase de arquitectura. Luego serán los detalles quienes implementen esas interfaces y provean una definición concreta para que en tiempo de ejecución el sistema pueda resolver los requerimientos end-to-end.

### Draft

`Expedition.draft` recibe el id desde afuera y no hay `ExpeditionRepository`: la expedición existe solo mientras quien llama conserve la referencia. `responsibles` es una lista de `UUID` que no se verifica contra el catálogo, así que se puede crear una expedición con responsables que no existen.

### AddActivity / AddDependency

Es la parte mejor protegida del modelo: `Itinerary` valida predecesores conocidos, ausencia de ciclos y orden temporal, y `Expedition` valida zona y período. `reorderActivities` cambia el orden de una lista que no tiene significado para el negocio (el orden real lo dan las dependencias y las ventanas).

### AddAssignment / AddPermit

El agregado solo verifica que la actividad exista y que la asignación no esté repetida. Ni el `personId` ni el `permitId` se verifican contra el catálogo al asignar: un id inexistente recién aparece cuando se valida, si se valida.

Además se pueden editar asignaciones en `IN_REVIEW`, después de validar, así que el `ValidationResult` que después se le pasa a `approve` ya no describe la expedición.

### SuggestAssignments

Bien: `AssignmentSuggester` devuelve sugerencias y no modifica la expedición, así que la decisión queda en quien llama.

Cuidado:

```java
public List<Assignment> suggest(
        Expedition expedition,
        Catalog catalog,
        List<Expedition> others) {
    Objects.requireNonNull(expedition, "expedition");
    Objects.requireNonNull(catalog, "catalog");
    // ...
}
```

Estas validaciones deberían aplicarse sobre los modelos en sí. No deberían poder crearse siquiera expediciones o catálogos nulos, porque no son valores que admite el dominio en la realidad. En consecuencia encontramos este tipo de validaciones en servicios: además de la responsabilidad que ya tiene el servicio, le agregamos la de validar que las entidades del dominio son válidas.

Smell:

```java
private static boolean heldBy(
        List<Assignment> current,
        Catalog catalog,
        UUID certificationId) {
    return current.stream().flatMap(
            assignment -> assignment instanceof PersonAssignment person
                    ? catalog.person(person.personId()).stream()
                    : Stream.empty()
    ).anyMatch(person -> person.holds(certificationId));
}
```

Para determinar cuáles de los assignments son aplicables a personas, para determinar quién es el actual encargado.

Esto es un smell porque nos comunica que estamos definiendo la identidad del objeto basado en su nombre y no en su contenido. En clase vimos ejemplos de patrón strategy + factory para escapar de este smell y lograr que el compilador no necesite conocer el tipo explícito. Alternativamente, si se contara con un aggregate como podría ser `CurrentAssignments` que agrupe los assignments presentes, se podría implementar un método `getPersonAssignments()` que retorne dentro de los assignments aquellos que son de tipo person. Idealmente el filtrado no debería realizarse con `instanceof`, sino comparando contra un atributo que solo contenga `PersonAssignment`, o un método `appliesFor()` que nos ayude para definir aplicabilidad.

Cabe resaltar que las implementaciones de `Assignment` utilizan `UUID` para representar conceptos que podrían definirse de forma que comuniquen mucha más información, como `PersonId`, `VehicleId`, etc.

Otro smell: `Assignment` tiene un método default que no aplica para todos:

```java
default Map<UUID, Quantity> consumption() { return Map.of(); }
```

3 de 4 implementaciones no son aplicables puesto que no tienen `Quantity`: terminan con un default que retorna vacío.

`ConsumableAssignment` es el único que sobreescribe este método: es una clara violación de ISP.

### SubmitForReview

Transición simple sin validación: se puede enviar a revisión una expedición sin actividades, y como las reglas recorren las actividades, ninguna la rechaza.

### Validate

Buen Strategy + composición de `ValidationRule`. El problema no está acá sino en cómo se usa el resultado: es un valor suelto que no guarda a qué versión ni a qué momento de la expedición corresponde.

### AcceptWarning

`AcceptedWarning` rechaza críticos y exige justificación, pero `acceptedBy` es cualquier `UUID`: no se compara con `responsibles`, cuando el enunciado dice "aceptadas por un responsable".

### Approve

Ver punto 1: validar y aprobar son dos pasos sin nada que los ate. El agregado nunca ejecuta al validador, solo lee el objeto que le entregan.

### Start / Suspend / Resume / Finish

`suspend()` también vale desde `APPROVED`, y `resume()` siempre vuelve a `IN_PROGRESS`: una expedición que nunca se inició queda en ejecución sin pasar por `start()`. `SUSPENDED` no recuerda desde qué estado se suspendió.

### StartActivity / FinishActivity

Buen uso de un objeto inmutable (`ActivityExecution`) y respeta los predecesores. Pero el instante lo pasa quien llama (no hay `Clock` inyectado) y no se compara con la ventana ni con el período: se puede registrar un inicio en 1990 o en 2090.

### Replanner (Cancel / Delay / ReplaceUnavailable)

Bien: una expedición aprobada no se edita, se versiona, y `delay` calcula, valida y después aplica. Sin embargo:

- Quien llama tiene que acordarse de guardar el plan y de volver a enviarlo, validarlo y aprobarlo. La versión original sigue ocupando recursos para las demás y nada la saca de circulación.
- Doble semántica: según el estado, modifica el argumento o devuelve otra instancia (`revisionOrSelf`).
- `replaceUnavailable` no vuelve a `DRAFT` como `cancel` y `delay`: en `IN_REVIEW` cambia asignaciones y conserva las advertencias aceptadas, y un `ValidationResult` previo sigue sirviendo para `approve`.

### ReturnToDraft

Desde `IN_PROGRESS` hace `executions.clear()` y borra el registro de lo ejecutado. `Replanner` usa `reviseAsDraft` y evita el problema, pero el método público del agregado deja el camino destructivo abierto.

### OperationalReport

"Duración" y "consumo" salen del plan, no de lo ejecutado: el informe de una expedición terminada no dice cuánto duró ni qué consumió, aunque `ActivityExecution` tiene `startedAt` / `finishedAt`.

## 2. Separación negocio-detalles

Java puro y sin frameworks. `Catalog` es un puerto, pero su implementación en memoria `ResourceCatalog` (5 `Map<UUID, …>`) vive en `domain.catalog`: es un detalle dentro del dominio.

`Catalog` es una interfaz que cubre muchas responsabilidades. Parece un repositorio que engloba todas las entidades de la aplicación, lo cual es un super smell.

La responsabilidad de persistir las entidades dentro de un bounded context se puede atribuir generalmente a un repositorio por bounded context, extensible según se requiera. El código tiene una buena separación de subdominios. La pregunta sería por qué decidieron ignorar esta separación para el repositorio y hacer una God class como lo es `ResourceCatalog`.

## 3. SOLID

- **OCP bien:** el validador es una lista de `ValidationRule` con un solo método, y `ValidationExtensionTest` lo demuestra.
- **OCP mal:** `ActivityPolicy` y `Assignment` son `sealed`, y hay 7 `switch` / `instanceof` por tipo (`TemporalBooking` x2, `CapacityRule` x2, `MissingResourceRule`, `CertificationRule`, `AssignmentSuggester`). Un nuevo `Assignment` obliga a tocarlos todos, y el `DESIGN.md` no justifica `sealed` frente a OCP. Lo polimórfico sería que cada `Assignment` sepa producir su `booking(window)` y verificarse.
- **SRP:** `Expedition` tiene demasiados actores (ver punto 1).
- **DIP:** bien.
- **ISP:** se encontraron violaciones como se vio con el método default `consumption()` en `Assignment.java`.

## 4. Patrones

Strategy + lista inyectada en `ExpeditionValidator`, con `ValidationContext` para unificar las firmas: bien.

State descartado: se justifica, pero `ExpeditionStatus` tiene 6 predicados del tipo `this == A || this == B || ...`, y cada estado nuevo toca todos.

## 5. Arquitectura

Como se vio en clase, el dominio contiene modelos, servicios, excepciones y las interfaces (puertos de salida y casos de uso), y las implementaciones concretas viven afuera. Acá hay un único módulo `domain` sin casos de uso (interactors) ni puertos driving, y con un adaptador (`ResourceCatalog`) adentro.

Falta uso de aggregates y modelar IDs de las entidades de cada subdominio. A pesar de esto, los subdominios están bien identificados: se puede ver una clara composición del negocio con los módulos propuestos. Solo restaría dentro de ellos hacer la separación en modelos, interfaces, repositorios, use cases, etc.
