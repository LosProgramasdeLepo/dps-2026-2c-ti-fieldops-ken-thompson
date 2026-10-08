package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.tracking.ActivityExecution;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.domain.tracking.InvalidActivityExecution;
import edu.itba.fieldops.domain.tracking.Observation;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpeditionExecutionTest {
    private static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");

    private final ExpeditionExecution execution = ExpeditionExecution.started(new ExpeditionId(UUID.randomUUID()));
    private final ActivityId first = new ActivityId(UUID.randomUUID());
    private final ActivityId second = new ActivityId(UUID.randomUUID());

    @Test
    void cannotStartAnActivityBeforeItsPredecessorFinishes() {
        execution.startActivity(first, DAY, Set.of());

        assertThrows(InvalidActivityExecution.class, () -> execution.startActivity(second, hours(4), Set.of(first)));
    }

    @Test
    void startsAnActivityOnceItsPredecessorFinished() {
        execution.startActivity(first, DAY, Set.of());
        execution.finishActivity(first, hours(4), "site reached");

        execution.startActivity(second, hours(4), Set.of(first));

        assertTrue(execution.hasStarted(second));
    }

    @Test
    void finishingAnActivityRecordsWhenAndWithWhatResult() {
        execution.startActivity(first, DAY, Set.of());

        execution.finishActivity(first, hours(1), "samples stored");

        ActivityExecution finished = execution.activities().getFirst();
        assertAll(
                () -> assertEquals(Optional.of(hours(1)), finished.finishedAt()),
                () -> assertEquals(Optional.of("samples stored"), finished.result())
        );
    }

    @Test
    void cannotFinishAnActivityTwice() {
        execution.startActivity(first, DAY, Set.of());
        execution.finishActivity(first, hours(1), "samples stored");

        assertThrows(InvalidActivityExecution.class, () -> execution.finishActivity(first, hours(2), "samples stored again"));
    }

    @Test
    void theRecordedActivitiesCannotBeChangedFromOutside() {
        execution.startActivity(first, DAY, Set.of());

        execution.activities().getFirst().finish(hours(1), "samples stored");

        assertFalse(execution.activities().getFirst().isFinished());
    }

    @Test
    void finishesOnceEveryPlannedActivityFinished() {
        execution.startActivity(first, DAY, Set.of());
        execution.finishActivity(first, hours(1), "samples stored");

        execution.finish(Set.of(first));

        assertEquals(ExpeditionExecution.Status.FINISHED, execution.status());
    }

    @Test
    void cannotFinishWhileAPlannedActivityHasNotStarted() {
        assertThrows(InvalidActivityExecution.class, () -> execution.finish(Set.of(first)));
    }

    @Test
    void cannotFinishWhileAnActivityIsRunning() {
        execution.startActivity(first, DAY, Set.of());

        assertThrows(InvalidActivityExecution.class, () -> execution.finish(Set.of(first)));
    }

    @Test
    void onlyARunInProgressCanBeSuspended() {
        execution.suspend();

        assertEquals(ExpeditionExecution.Status.SUSPENDED, execution.status());
        assertThrows(InvalidExpeditionTransition.class, execution::suspend);
    }

    @Test
    void resumingASuspendedRunPutsItBackInProgress() {
        execution.suspend();

        execution.resume();

        assertEquals(ExpeditionExecution.Status.IN_PROGRESS, execution.status());
    }

    @Test
    void recordsIncidentsAndObservations() {
        Incident incident = Incident.of("rain delay", DAY);
        Observation observation = new Observation("site wet", DAY);

        execution.addIncident(incident);
        execution.addObservation(observation);

        assertAll(
                () -> assertEquals(List.of(incident), execution.incidents()),
                () -> assertEquals(List.of(observation), execution.observations())
        );
    }

    @Test
    void aRestoredRunHasTheStateItWasStoredWith() {
        execution.startActivity(first, DAY, Set.of());
        execution.finishActivity(first, hours(4), "site reached");
        execution.startActivity(second, hours(4), Set.of(first));
        execution.addIncident(Incident.affecting(second, "flooded trail", hours(5)));
        execution.addObservation(new Observation("heron colony", hours(5)));
        execution.suspend();

        ExpeditionExecution restored = ExpeditionExecution.restore(execution.state());
        restored.resume();

        assertAll(
                () -> assertEquals(execution.activities(), restored.activities()),
                () -> assertEquals(execution.incidents(), restored.incidents()),
                () -> assertEquals(execution.observations(), restored.observations()),
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

    private static Instant hours(int hours) {
        return DAY.plus(Duration.ofHours(hours));
    }
}
