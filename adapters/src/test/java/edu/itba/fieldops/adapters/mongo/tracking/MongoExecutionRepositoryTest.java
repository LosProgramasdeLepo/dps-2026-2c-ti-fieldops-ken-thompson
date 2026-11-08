package edu.itba.fieldops.adapters.mongo.tracking;

import edu.itba.fieldops.adapters.mongo.MongoRepositoryTest;
import edu.itba.fieldops.domain.expedition.ExecutionState;
import edu.itba.fieldops.domain.expedition.ExpeditionExecution;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.IncidentId;
import edu.itba.fieldops.domain.shared.Consumption;
import edu.itba.fieldops.domain.tracking.ActivityExecution;
import edu.itba.fieldops.domain.tracking.ActivityReport;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.domain.tracking.IncidentDescription;
import edu.itba.fieldops.domain.tracking.Observation;
import edu.itba.fieldops.domain.tracking.ObservationText;
import edu.itba.fieldops.domain.tracking.Outcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@MongoRepositoryTest
class MongoExecutionRepositoryTest {
    private static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");

    @Autowired
    private MongoTemplate mongo;
    @Autowired
    private RunMongoRepository runDocuments;

    private MongoExecutionRepository runs;
    private final ExpeditionId expeditionId = new ExpeditionId(UUID.randomUUID());
    private final ActivityId soil = new ActivityId(UUID.randomUUID());
    private final ActivityId ride = new ActivityId(UUID.randomUUID());
    private final ConsumableId fuel = new ConsumableId(UUID.randomUUID());

    @BeforeEach
    void connect() {
        mongo.getDb().drop();
        runs = new MongoExecutionRepository(runDocuments);
    }

    @Test
    void aSuspendedRunKeepsItsActivitiesIncidentsAndObservations() {
        ExpeditionExecution run = ExpeditionExecution.restore(new ExecutionState(
                expeditionId,
                ExpeditionExecution.Status.IN_PROGRESS,
                List.of(
                        new ActivityExecution(soil, DAY).finish(
                                hours(4),
                                new ActivityReport(new Outcome("twelve samples"), Map.of(fuel, new Consumption(3)))
                        ),
                        new ActivityExecution(ride, hours(4))
                ),
                List.of(
                        new Incident(runs.nextIncidentId(), new IncidentDescription("flooded trail"), hours(5), Optional.of(ride)),
                        new Incident(runs.nextIncidentId(), new IncidentDescription("radio down"), hours(5), Optional.empty())
                ),
                List.of(new Observation(new ObservationText("heron colony"), hours(5)))
        ));
        run.suspend();

        runs.save(run);

        ExpeditionExecution stored = runs.find(expeditionId).orElseThrow();
        assertAll(
                () -> assertEquals(ExpeditionExecution.Status.SUSPENDED, stored.status()),
                () -> assertEquals(List.of(soil, ride), stored.activities().stream().map(ActivityExecution::activityId).toList()),
                () -> assertEquals(Optional.of(hours(4)), stored.activities().getFirst().finishedAt()),
                () -> assertEquals(Optional.of(new Outcome("twelve samples")), stored.activities().getFirst().result()),
                () -> assertEquals(Map.of(fuel, new Consumption(3)), stored.activities().getFirst().consumed()),
                () -> assertEquals(Optional.empty(), stored.activities().get(1).finishedAt()),
                () -> assertEquals(run.incidents(), stored.incidents()),
                () -> assertEquals(run.observations(), stored.observations())
        );
    }

    @Test
    void incidentsComeBackWithTheIdTheyWereStoredWith() {
        IncidentId first = runs.nextIncidentId();
        IncidentId second = runs.nextIncidentId();
        runs.save(ExpeditionExecution.restore(new ExecutionState(
                expeditionId,
                ExpeditionExecution.Status.IN_PROGRESS,
                List.of(),
                List.of(
                        new Incident(first, new IncidentDescription("flooded trail"), hours(5), Optional.empty()),
                        new Incident(second, new IncidentDescription("radio down"), hours(6), Optional.empty())
                ),
                List.of()
        )));

        List<IncidentId> stored = runs.find(expeditionId).orElseThrow().incidents().stream().map(Incident::id).toList();

        assertEquals(List.of(first, second), stored);
    }

    @Test
    void aDocumentWrittenBeforeIncidentIdsGetsTheSameIdOnEveryRead() {
        runDocuments.save(new RunDocument(
                expeditionId.value().toString(),
                "IN_PROGRESS",
                List.of(),
                List.of(
                        new IncidentDocument(null, "flooded trail", hours(5), null),
                        new IncidentDocument(null, "radio down", hours(6), null)
                ),
                List.of()
        ));

        List<Incident> firstRead = runs.find(expeditionId).orElseThrow().incidents();
        List<Incident> secondRead = runs.find(expeditionId).orElseThrow().incidents();

        assertAll(
                () -> assertEquals(firstRead, secondRead),
                () -> assertNotEquals(firstRead.get(0).id(), firstRead.get(1).id())
        );
    }

    @Test
    void aPlanWithoutARunHasNone() {
        assertEquals(Optional.empty(), runs.find(expeditionId));
    }

    private static Instant hours(int hours) {
        return DAY.plus(Duration.ofHours(hours));
    }
}
