package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.ActivityBlock;
import edu.itba.fieldops.domain.itinerary.EstimatedDuration;
import edu.itba.fieldops.domain.itinerary.InvalidItinerary;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.support.StoredPeople;
import edu.itba.fieldops.domain.support.StoredPlans;
import edu.itba.fieldops.domain.tracking.ActivityExecution;
import edu.itba.fieldops.domain.tracking.ActivityReport;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.domain.tracking.InvalidActivityExecution;
import edu.itba.fieldops.domain.tracking.Observation;
import edu.itba.fieldops.domain.tracking.Outcome;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static edu.itba.fieldops.domain.support.Submissions.submit;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpeditionExecutionTest {
    private static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");
    private static final TimePeriod PERIOD = new TimePeriod(DAY, DAY.plus(Duration.ofDays(5)));
    private static final WorkZone DELTA = new WorkZone("Delta");

    private final ExpeditionId expeditionId = new ExpeditionId(UUID.randomUUID());
    private final ExpeditionExecution execution = open(expeditionId);
    private final ActivityId first = new ActivityId(UUID.randomUUID());
    private final ActivityId second = new ActivityId(UUID.randomUUID());

    @Test
    void cannotStartAnActivityBeforeItsPredecessorFinishes() {
        RunOnPlan started = running(activity(first, 0, 4), activity(second, 4, 8).after(Set.of(first)));
        started.run.startActivity(started.plan.startOf(first, DAY, List.of(started.plan)));

        assertThrows(
                InvalidActivityExecution.class,
                () -> started.run.startActivity(started.plan.startOf(second, hours(4), List.of(started.plan)))
        );
    }

    @Test
    void startsAnActivityOnceItsPredecessorFinished() {
        RunOnPlan started = running(activity(first, 0, 4), activity(second, 4, 8).after(Set.of(first)));
        started.run.startActivity(started.plan.startOf(first, DAY, List.of(started.plan)));
        started.run.finishActivity(first, PERIOD.at(hours(4)), ActivityReport.of(new Outcome("site reached")));

        started.run.startActivity(started.plan.startOf(second, hours(4), List.of(started.plan)));

        assertTrue(started.run.hasStarted(second));
    }

    @Test
    void finishingAnActivityRecordsWhenAndWithWhatResult() {
        ExpeditionExecution running = open(expeditionId, new ActivityExecution(first, DAY));

        running.finishActivity(first, PERIOD.at(hours(1)), ActivityReport.of(new Outcome("samples stored")));

        ActivityExecution finished = running.activities().getFirst();
        assertAll(
                () -> assertEquals(Optional.of(hours(1)), finished.finishedAt()),
                () -> assertEquals(Optional.of(new Outcome("samples stored")), finished.result())
        );
    }

    @Test
    void cannotFinishAnActivityTwice() {
        ExpeditionExecution running = open(expeditionId, new ActivityExecution(first, DAY));
        running.finishActivity(first, PERIOD.at(hours(1)), ActivityReport.of(new Outcome("samples stored")));

        assertThrows(InvalidActivityExecution.class, () -> running.finishActivity(first, PERIOD.at(hours(2)), ActivityReport.of(new Outcome("samples stored again"))));
    }

    @Test
    void theRecordedActivitiesCannotBeChangedFromOutside() {
        ExpeditionExecution running = open(expeditionId, new ActivityExecution(first, DAY));

        running.activities().getFirst().finish(hours(1), ActivityReport.of(new Outcome("samples stored")));

        assertFalse(running.activities().getFirst().isFinished());
    }

    @Test
    void finishesOnceEveryPlannedActivityFinished() {
        RunOnPlan started = running(activity(first, 0, 4));
        started.run.startActivity(started.plan.startOf(first, DAY, List.of(started.plan)));
        started.run.finishActivity(first, PERIOD.at(hours(1)), ActivityReport.of(new Outcome("samples stored")));

        started.run.finish(started.plan);

        assertEquals(ExpeditionExecution.Status.FINISHED, started.run.status());
    }

    @Test
    void cannotFinishWhileAPlannedActivityHasNotStarted() {
        RunOnPlan started = running(activity(first, 0, 4));

        assertThrows(InvalidActivityExecution.class, () -> started.run.finish(started.plan));
    }

    @Test
    void cannotFinishWhileAnActivityIsRunning() {
        RunOnPlan started = running(activity(first, 0, 4));
        started.run.startActivity(started.plan.startOf(first, DAY, List.of(started.plan)));

        assertThrows(InvalidActivityExecution.class, () -> started.run.finish(started.plan));
    }

    @Test
    void aSequentialBlockWaitsForTheEarlierActivity() {
        Activity earlier = activity(first, 0, 4).build();
        Activity later = activity(second, 4, 8).build();
        Expedition plan = draftWithoutActivities();
        plan.addBlock(ActivityBlock.sequential(earlier, later));
        submit(plan);
        Expedition approved = StoredPlans.approved(plan);
        ExpeditionExecution run = approved.start(DAY);

        assertThrows(
                InvalidActivityExecution.class,
                () -> run.startActivity(approved.startOf(later.id(), hours(4), List.of(approved)))
        );

        assertFalse(run.hasStarted(later.id()));
    }

    @Test
    void anEmptyItineraryDoesNotCloseTheRun() {
        assertThrows(InvalidItinerary.class, () -> execution.finish(draftWithoutActivities(expeditionId)));

        assertEquals(ExpeditionExecution.Status.IN_PROGRESS, execution.status());
    }

    @Test
    void suspendsARunInProgress() {
        execution.suspend();

        assertEquals(ExpeditionExecution.Status.SUSPENDED, execution.status());
    }

    @Test
    void aSuspendedRunCannotBeSuspendedAgain() {
        execution.suspend();

        assertThrows(InvalidExpeditionTransition.class, execution::suspend);

        assertEquals(ExpeditionExecution.Status.SUSPENDED, execution.status());
    }

    @Test
    void resumingASuspendedRunPutsItBackInProgress() {
        execution.suspend();

        execution.resume();

        assertEquals(ExpeditionExecution.Status.IN_PROGRESS, execution.status());
    }

    @Test
    void recordsAnIncident() {
        Incident incident = Incident.of("rain delay", DAY);

        execution.addIncident(incident);

        assertEquals(List.of(incident), execution.incidents());
    }

    @Test
    void recordsAnObservation() {
        Observation observation = new Observation("site wet", DAY);

        execution.addObservation(observation);

        assertEquals(List.of(observation), execution.observations());
    }

    @Test
    void aRestoredRunHasTheStateItWasStoredWith() {
        ExpeditionExecution withActivities = open(
                expeditionId,
                new ActivityExecution(first, DAY).finish(hours(4), ActivityReport.of(new Outcome("site reached"))),
                new ActivityExecution(second, hours(4))
        );
        withActivities.addIncident(Incident.affecting(second, "flooded trail", hours(5)));
        withActivities.addObservation(new Observation("heron colony", hours(5)));
        withActivities.suspend();

        ExpeditionExecution restored = ExpeditionExecution.restore(withActivities.state());
        restored.resume();

        assertAll(
                () -> assertEquals(withActivities.activities(), restored.activities()),
                () -> assertEquals(withActivities.incidents(), restored.incidents()),
                () -> assertEquals(withActivities.observations(), restored.observations()),
                () -> assertEquals(ExpeditionExecution.Status.IN_PROGRESS, restored.status())
        );
    }

    @Test
    void rejectsRestoringAFinishedRunWithAnActivityStillRunning() {
        ExecutionState running = new ExecutionState(
                new ExpeditionId(UUID.randomUUID()),
                ExpeditionExecution.Status.FINISHED,
                List.of(new ActivityExecution(first, DAY)),
                List.of(),
                List.of()
        );

        assertThrows(InvalidActivityExecution.class, () -> ExpeditionExecution.restore(running));
    }

    @Test
    void rejectsRestoringARunThatStartedAnActivityTwice() {
        ExecutionState repeated = new ExecutionState(
                new ExpeditionId(UUID.randomUUID()),
                ExpeditionExecution.Status.IN_PROGRESS,
                List.of(new ActivityExecution(first, DAY), new ActivityExecution(first, hours(1))),
                List.of(),
                List.of()
        );

        assertThrows(InvalidActivityExecution.class, () -> ExpeditionExecution.restore(repeated));
    }

    private static ExpeditionExecution open(ExpeditionId id, ActivityExecution... activities) {
        return ExpeditionExecution.restore(new ExecutionState(
                id,
                ExpeditionExecution.Status.IN_PROGRESS,
                List.of(activities),
                List.of(),
                List.of()
        ));
    }

    private static RunOnPlan running(Activity.Builder... activities) {
        PersonId lead = new PersonId(UUID.randomUUID());
        Expedition plan = Expedition.draft(
                new ExpeditionId(UUID.randomUUID()),
                new ExpeditionCharter(
                        List.of(new Objective("Map wetland")),
                        PERIOD,
                        List.of(DELTA),
                        List.of(lead),
                        List.of(new Restriction("Daylight only"))
                ),
                StoredPeople.of(new Person(lead, "Ada", List.of(), Availability.always()))
        );
        for (Activity.Builder activity : activities) {
            plan.addActivity(activity.build());
        }
        submit(plan);
        Expedition approved = StoredPlans.approved(plan);
        return new RunOnPlan(approved, approved.start(DAY));
    }

    private static Activity.Builder activity(ActivityId id, int fromHour, int toHour) {
        return Activity.transit()
                .named(id, "survey")
                .estimated(new EstimatedDuration(Duration.ofHours(2)), RiskLevel.LOW)
                .in(DELTA, new TimePeriod(hours(fromHour), hours(toHour)));
    }

    private static Expedition draftWithoutActivities() {
        return draftWithoutActivities(new ExpeditionId(UUID.randomUUID()));
    }

    private static Expedition draftWithoutActivities(ExpeditionId id) {
        PersonId lead = new PersonId(UUID.randomUUID());
        return Expedition.draft(
                id,
                new ExpeditionCharter(
                        List.of(new Objective("Map wetland")),
                        PERIOD,
                        List.of(DELTA),
                        List.of(lead),
                        List.of(new Restriction("Daylight only"))
                ),
                StoredPeople.of(new Person(lead, "Ada", List.of(), Availability.always()))
        );
    }

    private static Instant hours(int hours) {
        return DAY.plus(Duration.ofHours(hours));
    }

    private record RunOnPlan(Expedition plan, ExpeditionExecution run) {
    }
}
