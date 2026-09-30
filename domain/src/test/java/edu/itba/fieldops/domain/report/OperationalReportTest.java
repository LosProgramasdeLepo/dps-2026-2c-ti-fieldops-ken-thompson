package edu.itba.fieldops.domain.report;

import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Certification;
import edu.itba.fieldops.domain.catalog.Instrument;
import edu.itba.fieldops.domain.catalog.Permit;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.details.ResourceCatalog;
import edu.itba.fieldops.domain.expedition.Approvals;
import edu.itba.fieldops.domain.expedition.ConsumableAssignment;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionEditing;
import edu.itba.fieldops.domain.expedition.InstrumentAssignment;
import edu.itba.fieldops.domain.expedition.Objective;
import edu.itba.fieldops.domain.expedition.PersonAssignment;
import edu.itba.fieldops.domain.expedition.Restriction;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.ActivityBlock;
import edu.itba.fieldops.domain.shared.InstrumentKind;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.Stock;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.tracking.ExpeditionExecution;
import edu.itba.fieldops.domain.tracking.Incident;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OperationalReportTest {

    private static final Instant START = Instant.parse("2026-11-01T08:00:00Z");
    private static final WorkZone DELTA = new WorkZone("Delta");
    private static final InstrumentKind PROBE = new InstrumentKind("probe");

    @Test
    void derivesDurationRiskAndConsumptionFromExpedition() {
        ConsumableId vials = new ConsumableId(UUID.randomUUID());
        Prepared prepared = draftWithMeasurement(Map.of(vials, new Stock(3)));
        Activity activity = prepared.activity();
        ExpeditionEditing.addAssignment(prepared.expedition(), new ConsumableAssignment(activity.id(), vials, new Stock(5)));
        ExpeditionEditing.addAssignment(prepared.expedition(), new ConsumableAssignment(activity.id(), vials, new Stock(2)));

        OperationalReport report = OperationalReport.of(prepared.expedition());

        assertAll(
                () -> assertEquals(OperationalStatus.DRAFT, report.status()),
                () -> assertEquals(1, report.plannedActivities()),
                () -> assertEquals(0, report.startedActivities()),
                () -> assertEquals(0, report.finishedActivities()),
                () -> assertEquals(Duration.ofHours(3), report.duration()),
                () -> assertEquals(RiskLevel.HIGH, report.risk()),
                () -> assertEquals(new Stock(7), report.consumption().get(vials)),
                () -> assertEquals(new Stock(3), report.estimatedConsumption().get(vials)),
                () -> assertEquals(List.of(), report.activityResults())
        );
    }

    @Test
    void reportReadsTheRaisedRiskStoredOnANightActivity() {
        CertificationId nightOperation = new CertificationId(UUID.randomUUID());
        Activity activity = Activity.night(
                new ActivityId(UUID.randomUUID()),
                "night survey",
                Duration.ofHours(2),
                RiskLevel.LOW,
                new TimePeriod(START, START.plus(Duration.ofHours(2))),
                Set.of(),
                DELTA,
                nightOperation,
                new InstrumentKind("lighting")
        );
        Expedition expedition = ExpeditionEditing.draft(
                new ExpeditionId(UUID.randomUUID()),
                List.of(new Objective("Watch the delta")),
                new TimePeriod(START, START.plus(Duration.ofDays(2))),
                List.of(DELTA),
                List.of(new PersonId(UUID.randomUUID())),
                List.of(new Restriction("Stay on the water"))
        );
        ExpeditionEditing.addActivity(expedition, activity);

        OperationalReport report = OperationalReport.of(expedition);

        assertAll(
                () -> assertEquals(RiskLevel.MEDIUM, report.risk()),
                () -> assertEquals(Duration.ofHours(2), report.duration()),
                () -> assertEquals(1, report.plannedActivities())
        );
    }

    @Test
    void reportCountsLeavesAndUsesTheTreeDurationOfANestedBlock() {
        Activity approach = Activity.transit(
                new ActivityId(UUID.randomUUID()),
                "approach",
                Duration.ofHours(2),
                RiskLevel.LOW,
                new TimePeriod(START, START.plus(Duration.ofHours(2))),
                Set.of(),
                DELTA
        );
        Activity left = Activity.sampling(
                new ActivityId(UUID.randomUUID()),
                "left",
                Duration.ofHours(4),
                RiskLevel.MEDIUM,
                new TimePeriod(START, START.plus(Duration.ofHours(4))),
                Set.of(),
                DELTA,
                new CertificationId(UUID.randomUUID())
        );
        Activity right = Activity.sampling(
                new ActivityId(UUID.randomUUID()),
                "right",
                Duration.ofHours(3),
                RiskLevel.HIGH,
                new TimePeriod(START, START.plus(Duration.ofHours(3))),
                Set.of(),
                DELTA,
                new CertificationId(UUID.randomUUID())
        );
        Expedition expedition = ExpeditionEditing.draft(
                new ExpeditionId(UUID.randomUUID()),
                List.of(new Objective("Survey the delta")),
                new TimePeriod(START, START.plus(Duration.ofDays(2))),
                List.of(DELTA),
                List.of(new PersonId(UUID.randomUUID())),
                List.of(new Restriction("Stay on the water"))
        );
        ExpeditionEditing.addBlock(expedition, ActivityBlock.sequential(approach, ActivityBlock.parallel(left, right)));

        OperationalReport report = OperationalReport.of(expedition);

        assertAll(
                () -> assertEquals(3, report.plannedActivities()),
                () -> assertEquals(Duration.ofHours(6), report.duration()),
                () -> assertEquals(RiskLevel.HIGH, report.risk())
        );
    }

    @Test
    void includesFinishedActivityResults() {
        Prepared prepared = draftWithMeasurement(Map.of());
        ExpeditionEditing.submitForReview(prepared.expedition());
        Approvals.approve(prepared.expedition(), prepared.catalog());
        ExpeditionExecution execution = ExpeditionExecution.started(prepared.expedition().id());
        execution.startActivity(prepared.activity().id(), START, prepared.expedition().activityOf(prepared.activity().id()).predecessors());
        execution.finishActivity(prepared.activity().id(), START.plus(Duration.ofHours(3)), "samples stored");

        OperationalReport report = OperationalReport.of(prepared.expedition(), execution);

        assertAll(
                () -> assertEquals(OperationalStatus.IN_PROGRESS, report.status()),
                () -> assertEquals(1, report.plannedActivities()),
                () -> assertEquals(1, report.startedActivities()),
                () -> assertEquals(1, report.finishedActivities()),
                () -> assertEquals(List.of(new ActivityResult(prepared.activity().id(), "samples stored")), report.activityResults())
        );
    }

    @Test
    void includesIncidents() {
        Prepared prepared = draftWithMeasurement(Map.of());
        ExpeditionEditing.submitForReview(prepared.expedition());
        Approvals.approve(prepared.expedition(), prepared.catalog());
        ExpeditionExecution execution = ExpeditionExecution.started(prepared.expedition().id());
        Incident incident = Incident.of("ventisca en el frente", START);
        execution.addIncident(incident);

        OperationalReport report = OperationalReport.of(prepared.expedition(), execution);

        assertEquals(List.of(incident), report.incidents());
        assertEquals(OperationalStatus.IN_PROGRESS, report.status());
    }

    @Test
    void reportsFinishedWhenTheRunIsFinished() {
        Prepared prepared = draftWithMeasurement(Map.of());
        ExpeditionEditing.submitForReview(prepared.expedition());
        Approvals.approve(prepared.expedition(), prepared.catalog());
        ExpeditionExecution execution = ExpeditionExecution.started(prepared.expedition().id());
        execution.startActivity(prepared.activity().id(), START, prepared.expedition().activityOf(prepared.activity().id()).predecessors());
        execution.finishActivity(prepared.activity().id(), START.plus(Duration.ofHours(3)), "samples stored");
        execution.finish(prepared.expedition().itinerary());

        OperationalReport report = OperationalReport.of(prepared.expedition(), execution);

        assertEquals(OperationalStatus.FINISHED, report.status());
    }

    private static Prepared draftWithMeasurement(Map<ConsumableId, Stock> estimated) {
        CertificationId certificationId = new CertificationId(UUID.randomUUID());
        PersonId personId = new PersonId(UUID.randomUUID());
        InstrumentId instrumentId = new InstrumentId(UUID.randomUUID());
        Activity activity = Activity.measurement(
                new ActivityId(UUID.randomUUID()),
                "measure",
                Duration.ofHours(3),
                RiskLevel.HIGH,
                new TimePeriod(START, START.plus(Duration.ofHours(3))),
                Set.of(),
                DELTA,
                certificationId,
                PROBE,
                estimated
        );
        Permit permit = Permit.zone(new PermitId(UUID.randomUUID()), DELTA, activity.window());
        Expedition expedition = ExpeditionEditing.draft(
                new ExpeditionId(UUID.randomUUID()),
                List.of(new Objective("Measure water")),
                new TimePeriod(START, START.plus(Duration.ofDays(2))),
                List.of(DELTA),
                List.of(new PersonId(UUID.randomUUID())),
                List.of(new Restriction("Daylight only"))
        );
        ExpeditionEditing.addActivity(expedition, activity);
        ExpeditionEditing.addAssignment(expedition, new PersonAssignment(activity.id(), personId));
        ExpeditionEditing.addAssignment(expedition, new InstrumentAssignment(activity.id(), instrumentId));
        ExpeditionEditing.addPermit(expedition, permit.id());
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(new Person(personId, "Ada", List.of(new Certification(certificationId, "Operator")), Availability.always()));
        catalog.add(new Instrument(instrumentId, PROBE, Availability.always()));
        catalog.add(permit);
        return new Prepared(expedition, catalog, activity);
    }

    private record Prepared(Expedition expedition, ResourceCatalog catalog, Activity activity) {
    }
}
