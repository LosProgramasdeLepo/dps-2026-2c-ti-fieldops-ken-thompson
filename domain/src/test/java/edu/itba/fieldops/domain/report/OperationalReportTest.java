package edu.itba.fieldops.domain.report;

import edu.itba.fieldops.domain.catalog.Availability;
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
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.Stock;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.tracking.ActivityExecution;
import edu.itba.fieldops.domain.tracking.Incident;
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
            List.of(new Person(lead, "Ada", List.of(), Availability.always()))
    );
    private final ExpeditionExecution execution = open();

    @Test
    void aPlanWithoutARunReportsTheEstimateAndTheAssignedConsumption() {
        ConsumableId vials = new ConsumableId(UUID.randomUUID());
        Activity measure = activity(0, 3, RiskLevel.HIGH, Map.of(vials, new Stock(3)));
        expedition.addActivity(measure);
        expedition.addAssignment(new ConsumableAssignment(measure.id(), vials, new Stock(5)));
        expedition.addAssignment(new ConsumableAssignment(measure.id(), vials, new Stock(2)));

        OperationalReport report = OperationalReport.of(expedition);

        assertAll(
                () -> assertEquals(OperationalStatus.DRAFT, report.status()),
                () -> assertEquals(1, report.plannedActivities()),
                () -> assertEquals(0, report.startedActivities()),
                () -> assertEquals(Duration.ofHours(3), report.duration()),
                () -> assertEquals(RiskLevel.HIGH, report.risk()),
                () -> assertEquals(new Stock(7), report.consumption().get(vials)),
                () -> assertEquals(new Stock(3), report.estimatedConsumption().get(vials))
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
                new ActivityExecution(left.id(), START),
                new ActivityExecution(right.id(), START)
        );
        execution.finishActivity(left.id(), START.plus(Duration.ofHours(5)), "left bank surveyed");
        execution.finishActivity(right.id(), START.plus(Duration.ofHours(2)), "right bank surveyed");

        OperationalReport report = OperationalReport.of(expedition, execution);

        assertEquals(Duration.ofHours(5), report.duration());
    }

    @Test
    void includesTheResultsOfFinishedActivities() {
        Activity measure = activity(0, 3, RiskLevel.LOW);
        expedition.addActivity(measure);
        ExpeditionExecution execution = open(new ActivityExecution(measure.id(), START));
        execution.finishActivity(measure.id(), START.plus(Duration.ofHours(3)), "samples stored");

        OperationalReport report = OperationalReport.of(expedition, execution);

        assertAll(
                () -> assertEquals(OperationalStatus.IN_PROGRESS, report.status()),
                () -> assertEquals(1, report.startedActivities()),
                () -> assertEquals(1, report.finishedActivities()),
                () -> assertEquals(List.of(new ActivityResult(measure.id(), "samples stored")), report.activityResults())
        );
    }

    @Test
    void includesIncidents() {
        expedition.addActivity(activity(0, 3, RiskLevel.LOW));
        Incident incident = Incident.of("ventisca en el frente", START);
        execution.addIncident(incident);

        OperationalReport report = OperationalReport.of(expedition, execution);

        assertEquals(List.of(incident), report.incidents());
    }

    @Test
    void reportsFinishedWhenTheRunIsFinished() {
        Activity measure = activity(0, 3, RiskLevel.LOW);
        expedition.addActivity(measure);
        ExpeditionExecution execution = open(new ActivityExecution(measure.id(), START));
        execution.finishActivity(measure.id(), START.plus(Duration.ofHours(3)), "samples stored");
        execution.finish(expedition);

        OperationalReport report = OperationalReport.of(expedition, execution);

        assertEquals(OperationalStatus.FINISHED, report.status());
    }

    private ExpeditionExecution open(ActivityExecution... activities) {
        return ExpeditionExecution.restore(new ExecutionState(
                expedition.id(),
                ExpeditionExecution.Status.IN_PROGRESS,
                List.of(activities),
                List.of(),
                List.of()
        ));
    }

    private static Activity activity(int fromHour, int toHour, RiskLevel risk) {
        return activity(fromHour, toHour, risk, Map.of());
    }

    private static Activity activity(int fromHour, int toHour, RiskLevel risk, Map<ConsumableId, Stock> consumption) {
        return Activity.transit()
                .named(new ActivityId(UUID.randomUUID()), "survey")
                .estimated(Duration.ofHours(toHour - fromHour), risk)
                .in(DELTA, new TimePeriod(START.plus(Duration.ofHours(fromHour)), START.plus(Duration.ofHours(toHour))))
                .consuming(consumption)
                .build();
    }
}
