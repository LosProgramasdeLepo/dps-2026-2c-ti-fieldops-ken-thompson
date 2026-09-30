package edu.itba.fieldops.domain.report;

import edu.itba.fieldops.domain.expedition.ConsumableAssignment;
import edu.itba.fieldops.domain.expedition.ExecutionEditing;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionCharter;
import edu.itba.fieldops.domain.expedition.ExpeditionEditing;
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

    private final Expedition expedition = ExpeditionEditing.draft(
            new ExpeditionId(UUID.randomUUID()),
            new ExpeditionCharter(
                    List.of(new Objective("Survey the delta")),
                    new TimePeriod(START, START.plus(Duration.ofDays(2))),
                    List.of(DELTA),
                    List.of(new PersonId(UUID.randomUUID())),
                    List.of(new Restriction("Stay on the water"))
            )
    );
    private final ExpeditionExecution execution = ExecutionEditing.started(expedition.id());

    @Test
    void aPlanWithoutARunReportsTheEstimateAndTheAssignedConsumption() {
        ConsumableId vials = new ConsumableId(UUID.randomUUID());
        Activity measure = activity(0, 3, RiskLevel.HIGH, Map.of(vials, new Stock(3)));
        ExpeditionEditing.addActivity(expedition, measure);
        ExpeditionEditing.addAssignment(expedition, new ConsumableAssignment(measure.id(), vials, new Stock(5)));
        ExpeditionEditing.addAssignment(expedition, new ConsumableAssignment(measure.id(), vials, new Stock(2)));

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
        ExpeditionEditing.addBlock(expedition, ActivityBlock.sequential(approach, ActivityBlock.parallel(left, right)));

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
        ExpeditionEditing.addBlock(expedition, ActivityBlock.parallel(left, right));
        ExecutionEditing.startActivity(execution, left.id(), START, Set.of());
        ExecutionEditing.startActivity(execution, right.id(), START, Set.of());
        ExecutionEditing.finishActivity(execution, left.id(), START.plus(Duration.ofHours(5)), "left bank surveyed");
        ExecutionEditing.finishActivity(execution, right.id(), START.plus(Duration.ofHours(2)), "right bank surveyed");

        OperationalReport report = OperationalReport.of(expedition, execution);

        assertEquals(Duration.ofHours(5), report.duration());
    }

    @Test
    void includesTheResultsOfFinishedActivities() {
        Activity measure = activity(0, 3, RiskLevel.LOW);
        ExpeditionEditing.addActivity(expedition, measure);
        ExecutionEditing.startActivity(execution, measure.id(), START, Set.of());
        ExecutionEditing.finishActivity(execution, measure.id(), START.plus(Duration.ofHours(3)), "samples stored");

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
        ExpeditionEditing.addActivity(expedition, activity(0, 3, RiskLevel.LOW));
        Incident incident = Incident.of("ventisca en el frente", START);
        ExecutionEditing.addIncident(execution, incident);

        OperationalReport report = OperationalReport.of(expedition, execution);

        assertEquals(List.of(incident), report.incidents());
    }

    @Test
    void reportsFinishedWhenTheRunIsFinished() {
        Activity measure = activity(0, 3, RiskLevel.LOW);
        ExpeditionEditing.addActivity(expedition, measure);
        ExecutionEditing.startActivity(execution, measure.id(), START, Set.of());
        ExecutionEditing.finishActivity(execution, measure.id(), START.plus(Duration.ofHours(3)), "samples stored");
        ExecutionEditing.finish(execution, Set.of(measure.id()));

        OperationalReport report = OperationalReport.of(expedition, execution);

        assertEquals(OperationalStatus.FINISHED, report.status());
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
