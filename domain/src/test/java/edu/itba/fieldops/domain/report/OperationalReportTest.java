package edu.itba.fieldops.domain.report;

import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Consumable;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.expedition.ConsumableAssignment;
import edu.itba.fieldops.domain.expedition.ExecutionState;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionCharter;
import edu.itba.fieldops.domain.expedition.ExpeditionExecution;
import edu.itba.fieldops.domain.expedition.Objective;
import edu.itba.fieldops.domain.expedition.Restriction;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.ActivityBlock;
import edu.itba.fieldops.domain.itinerary.EstimatedDuration;
import edu.itba.fieldops.domain.shared.Consumption;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.Stock;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.support.ResourceCatalog;
import edu.itba.fieldops.domain.support.StoredPeople;
import edu.itba.fieldops.domain.tracking.ActivityExecution;
import edu.itba.fieldops.domain.tracking.ActivityReport;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.domain.tracking.Outcome;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OperationalReportTest {
    private static final Instant START = Instant.parse("2026-11-01T08:00:00Z");
    private static final WorkZone DELTA = new WorkZone("Delta");

    private final PersonId lead = new PersonId(UUID.randomUUID());
    private final Expedition expedition = Expedition.draft(
            new ExpeditionId(UUID.randomUUID()),
            new ExpeditionCharter(
                    List.of(new Objective("Survey the delta")),
                    new TimePeriod(START, START.plus(Duration.ofDays(2))),
                    List.of(DELTA),
                    List.of(lead),
                    List.of(new Restriction("Stay on the water"))
            ),
            StoredPeople.of(new Person(lead, "Ada", List.of(), Availability.always()))
    );

    @Test
    void aPlanWithoutARunReportsTheEstimateAndTheAssignedConsumption() {
        ConsumableId vials = new ConsumableId(UUID.randomUUID());
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(new Consumable(vials, "vials", new Stock(10)));
        Activity measure = activity(0, 3, RiskLevel.HIGH, Map.of(vials, new Consumption(3)));
        expedition.addActivity(measure);
        expedition.addAssignment(new ConsumableAssignment(measure.id(), vials, new Consumption(5)), catalog.catalogs());
        expedition.addAssignment(new ConsumableAssignment(measure.id(), vials, new Consumption(2)), catalog.catalogs());

        OperationalReport report = OperationalReport.of(expedition);

        assertAll(
                () -> assertEquals(OperationalStatus.DRAFT, report.status()),
                () -> assertEquals(1, report.plannedActivities()),
                () -> assertEquals(0, report.startedActivities()),
                () -> assertEquals(Duration.ofHours(3), report.duration()),
                () -> assertEquals(RiskLevel.HIGH, report.risk()),
                () -> assertEquals(new Consumption(7), report.consumption().get(vials)),
                () -> assertEquals(new Consumption(3), report.estimatedConsumption().get(vials))
        );
    }

    @Test
    void countsTheLeavesAndEstimatesTheTreeOfANestedBlock() {
        Activity approach = activity(0, 2, RiskLevel.LOW);
        Activity left = activity(2, 6, RiskLevel.MEDIUM);
        Activity right = activity(2, 5, RiskLevel.HIGH);
        expedition.addBlock(ActivityBlock.sequential(approach, ActivityBlock.parallel(left, right)));

        OperationalReport report = OperationalReport.of(expedition);

        assertAll(
                () -> assertEquals(3, report.plannedActivities()),
                () -> assertEquals(Duration.ofHours(6), report.duration()),
                () -> assertEquals(RiskLevel.HIGH, report.risk())
        );
    }

    @Test
    void theActualDurationOfAParallelBlockIsItsLongestBranch() {
        Activity left = activity(0, 4, RiskLevel.LOW);
        Activity right = activity(0, 3, RiskLevel.LOW);
        expedition.addBlock(ActivityBlock.parallel(left, right));
        ExpeditionExecution execution = open(
                finished(left, 5, "left bank surveyed"),
                finished(right, 2, "right bank surveyed")
        );

        OperationalReport report = OperationalReport.of(expedition, execution);

        assertEquals(Duration.ofHours(5), report.duration());
    }

    @Test
    void includesTheResultsOfFinishedActivities() {
        Activity measure = activity(0, 3, RiskLevel.LOW);
        expedition.addActivity(measure);
        ExpeditionExecution execution = open(finished(measure, 3, "samples stored"));

        OperationalReport report = OperationalReport.of(expedition, execution);

        assertAll(
                () -> assertEquals(OperationalStatus.IN_PROGRESS, report.status()),
                () -> assertEquals(1, report.startedActivities()),
                () -> assertEquals(1, report.finishedActivities()),
                () -> assertEquals(List.of(new ActivityResult(measure.id(), new Outcome("samples stored"))), report.activityResults())
        );
    }

    @Test
    void includesIncidents() {
        expedition.addActivity(activity(0, 3, RiskLevel.LOW));
        Incident incident = Incident.of("ventisca en el frente", START);
        ExpeditionExecution withIncident = ExpeditionExecution.restore(new ExecutionState(
                expedition.id(),
                ExpeditionExecution.Status.IN_PROGRESS,
                List.of(),
                List.of(incident),
                List.of()
        ));

        OperationalReport report = OperationalReport.of(expedition, withIncident);

        assertEquals(List.of(incident), report.incidents());
    }

    @Test
    void consumptionWithARunIsWhatTheFinishedActivitiesRecorded() {
        ConsumableId fuel = new ConsumableId(UUID.randomUUID());
        ConsumableId vials = new ConsumableId(UUID.randomUUID());
        Activity drive = activity(0, 2, RiskLevel.LOW, Map.of(fuel, new Consumption(10)));
        Activity measure = activity(2, 4, RiskLevel.LOW, Map.of(vials, new Consumption(3)));
        Activity pending = activity(4, 6, RiskLevel.LOW, Map.of(fuel, new Consumption(5)));
        expedition.addActivity(drive);
        expedition.addActivity(measure);
        expedition.addActivity(pending);
        ExpeditionExecution execution = open(
                finished(drive, 2, Map.of(fuel, new Consumption(12))),
                finished(measure, 4, Map.of(fuel, new Consumption(1), vials, new Consumption(2))),
                new ActivityExecution(pending.id(), START.plus(Duration.ofHours(4)))
        );

        OperationalReport report = OperationalReport.of(expedition, execution);

        assertAll(
                () -> assertEquals(Map.of(fuel, new Consumption(13), vials, new Consumption(2)), report.consumption()),
                () -> assertEquals(Map.of(fuel, new Consumption(15), vials, new Consumption(3)), report.estimatedConsumption())
        );
    }

    @Test
    void reportsFinishedWhenTheRunIsFinished() {
        Activity measure = activity(0, 3, RiskLevel.LOW);
        expedition.addActivity(measure);
        ExpeditionExecution execution = restored(ExpeditionExecution.Status.FINISHED, finished(measure, 3, "samples stored"));

        OperationalReport report = OperationalReport.of(expedition, execution);

        assertEquals(OperationalStatus.FINISHED, report.status());
    }

    private ExpeditionExecution open(ActivityExecution... activities) {
        return restored(ExpeditionExecution.Status.IN_PROGRESS, activities);
    }

    private ExpeditionExecution restored(ExpeditionExecution.Status status, ActivityExecution... activities) {
        return ExpeditionExecution.restore(new ExecutionState(
                expedition.id(),
                status,
                List.of(activities),
                List.of(),
                List.of()
        ));
    }

    private static ActivityExecution finished(Activity activity, int hours, String result) {
        return new ActivityExecution(activity.id(), START).finish(START.plus(Duration.ofHours(hours)), ActivityReport.of(new Outcome(result)));
    }

    private static ActivityExecution finished(Activity activity, int hours, Map<ConsumableId, Consumption> consumed) {
        return new ActivityExecution(activity.id(), START)
                .finish(START.plus(Duration.ofHours(hours)), new ActivityReport(new Outcome("done"), consumed));
    }

    private static Activity activity(int fromHour, int toHour, RiskLevel risk) {
        return activity(fromHour, toHour, risk, Map.of());
    }

    private static Activity activity(int fromHour, int toHour, RiskLevel risk, Map<ConsumableId, Consumption> consumption) {
        return Activity.transit()
                .named(new ActivityId(UUID.randomUUID()), "survey")
                .estimated(new EstimatedDuration(Duration.ofHours(toHour - fromHour)), risk)
                .in(DELTA, new TimePeriod(START.plus(Duration.ofHours(fromHour)), START.plus(Duration.ofHours(toHour))))
                .consuming(consumption)
                .build();
    }
}
