package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.support.StoredExpeditions;
import edu.itba.fieldops.domain.support.StoredPeople;
import edu.itba.fieldops.domain.support.StoredPlans;
import edu.itba.fieldops.domain.support.StoredRuns;
import edu.itba.fieldops.domain.tracking.InvalidActivityExecution;
import edu.itba.fieldops.domain.tracking.Outcome;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExpeditionTrackerTest {
    private static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");
    private static final WorkZone DELTA = new WorkZone("Delta");

    private final StoredRuns runs = new StoredRuns();
    private Instant now = DAY;

    @Test
    void aPlanDoesNotStartASecondRun() {
        Expedition plan = approved();
        ExpeditionTracker tracker = trackerOf(plan);
        runs.save(tracker.start(plan.id()));

        InvalidExpeditionTransition rejected = assertThrows(
                InvalidExpeditionTransition.class,
                () -> tracker.start(plan.id())
        );

        assertEquals("cannot start while expedition is IN_PROGRESS", rejected.getMessage());
    }

    @Test
    void aRevisionDoesNotStartAnotherRunOfTheLineage() {
        Expedition original = approved();
        Expedition revision = original.reviseAsDraft(new ExpeditionId(UUID.randomUUID()));
        revision.submitForReview();
        Expedition approvedRevision = StoredPlans.approved(revision);
        ExpeditionTracker tracker = trackerOf(original, approvedRevision);
        runs.save(tracker.start(original.id()));

        InvalidExpeditionTransition rejected = assertThrows(
                InvalidExpeditionTransition.class,
                () -> tracker.start(approvedRevision.id())
        );

        assertEquals("cannot start while expedition is IN_PROGRESS", rejected.getMessage());
    }

    @Test
    void anActivityDoesNotFinishAfterThePeriod() {
        Expedition plan = approved();
        ActivityId activity = plan.activities().getFirst().id();
        ExpeditionTracker tracker = trackerOf(plan);
        ExpeditionExecution run = tracker.start(plan.id());
        tracker.startActivity(run, activity);
        now = DAY.plus(Duration.ofDays(6));

        InvalidActivityExecution rejected = assertThrows(
                InvalidActivityExecution.class,
                () -> tracker.finishActivity(run, activity, new Outcome("samples stored"))
        );

        assertEquals("finish is outside the expedition period", rejected.getMessage());
        assertFalse(run.activities().getFirst().isFinished());
    }

    private ExpeditionTracker trackerOf(Expedition... plans) {
        return new ExpeditionTracker(new StoredExpeditions(plans), runs, () -> now);
    }

    private static Expedition approved() {
        PersonId lead = new PersonId(UUID.randomUUID());
        Expedition plan = Expedition.draft(
                new ExpeditionId(UUID.randomUUID()),
                new ExpeditionCharter(
                        List.of(new Objective("Map wetland")),
                        new TimePeriod(DAY, DAY.plus(Duration.ofDays(5))),
                        List.of(DELTA),
                        List.of(lead),
                        List.of(new Restriction("Daylight only"))
                ),
                StoredPeople.of(new Person(lead, "Ada", List.of(), Availability.always()))
        );
        plan.addActivity(Activity.transit()
                .named(new ActivityId(UUID.randomUUID()), "Approach")
                .estimated(Duration.ofHours(2), RiskLevel.LOW)
                .in(DELTA, new TimePeriod(DAY, DAY.plus(Duration.ofHours(4))))
                .build());
        plan.submitForReview();
        return StoredPlans.approved(plan);
    }
}
