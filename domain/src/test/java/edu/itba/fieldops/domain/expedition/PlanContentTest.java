package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.InvalidItinerary;
import edu.itba.fieldops.domain.shared.InstrumentKind;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanContentTest {
    private static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");
    private static final WorkZone DELTA = new WorkZone("Delta");

    @Test
    void acceptsOneActivityPerKind() {
        PlanContent plan = wetlandPlan();

        plan.addActivity(sampling());
        plan.addActivity(transit());
        plan.addActivity(measurement());

        assertEquals(
                List.of("Soil sampling", "Camp to site", "Water measurement"),
                plan.activities().stream().map(Activity::name).toList()
        );
    }

    @Test
    void rejectsActivityInUnknownZone() {
        PlanContent plan = wetlandPlan();
        Activity coast = transit("coast sample", 0, 2, new WorkZone("Coast"));

        assertThrows(InvalidItinerary.class, () -> plan.addActivity(coast));
    }

    @Test
    void rejectsActivityOutsidePeriod() {
        PlanContent plan = wetlandPlan();
        Activity late = transit("late", 200, 203, DELTA);

        assertThrows(InvalidItinerary.class, () -> plan.addActivity(late));
    }

    @Test
    void rejectsUnknownPredecessor() {
        PlanContent plan = wetlandPlan();
        Activity orphan = Activity.transit()
                .named(new ActivityId(UUID.randomUUID()), "dependent")
                .estimated(Duration.ofHours(2), RiskLevel.LOW)
                .in(DELTA, window(0, 2))
                .after(Set.of(new ActivityId(UUID.randomUUID())))
                .build();

        assertThrows(InvalidItinerary.class, () -> plan.addActivity(orphan));
    }

    @Test
    void addsDependencyWhenPredecessorFinishesBefore() {
        PlanContent plan = wetlandPlan();
        Activity first = sampling();
        Activity second = transit();
        plan.addActivity(first);
        plan.addActivity(second);

        plan.addDependency(second.id(), first.id());

        assertTrue(plan.activityOf(second.id()).predecessors().contains(first.id()));
    }

    @Test
    void rejectsCyclicDependency() {
        PlanContent plan = wetlandPlan();
        Activity first = sampling();
        Activity second = transit();
        Activity third = measurement();
        plan.addActivity(first);
        plan.addActivity(second);
        plan.addActivity(third);
        plan.addDependency(second.id(), first.id());
        plan.addDependency(third.id(), second.id());

        assertThrows(InvalidItinerary.class, () -> plan.addDependency(first.id(), third.id()));
    }

    @Test
    void rejectsPredecessorThatDoesNotFinishBefore() {
        PlanContent plan = wetlandPlan();
        Activity first = sampling();
        Activity overlapping = transit("overlap", 3, 5, DELTA);
        plan.addActivity(first);
        plan.addActivity(overlapping);

        assertThrows(InvalidItinerary.class, () -> plan.addDependency(overlapping.id(), first.id()));
    }

    @Test
    void rejectsAnActivityThatStartsBeforeItsPredecessorFinishes() {
        PlanContent plan = wetlandPlan();
        Activity first = sampling();
        plan.addActivity(first);
        Activity lateStart = Activity.transit()
                .named(new ActivityId(UUID.randomUUID()), "late start")
                .estimated(Duration.ofHours(2), RiskLevel.LOW)
                .in(DELTA, window(3, 5))
                .after(Set.of(first.id()))
                .build();

        assertThrows(InvalidItinerary.class, () -> plan.addActivity(lateStart));
    }

    @Test
    void removingPredecessorDropsTheDependency() {
        PlanContent plan = wetlandPlan();
        Activity first = sampling();
        Activity second = transit();
        plan.addActivity(first);
        plan.addActivity(second);
        plan.addDependency(second.id(), first.id());

        plan.removeActivity(first.id());

        assertAll(
                () -> assertEquals(List.of(second.id()), activityIds(plan)),
                () -> assertTrue(plan.activityOf(second.id()).predecessors().isEmpty())
        );
    }

    @Test
    void delayShiftsTheWholeChainOfDependents() {
        PlanContent plan = wetlandPlan();
        Activity first = sampling();
        Activity second = transit();
        Activity third = measurement();
        plan.addActivity(first);
        plan.addActivity(second);
        plan.addActivity(third);
        plan.addDependency(second.id(), first.id());
        plan.addDependency(third.id(), second.id());

        plan.delay(first.id(), Duration.ofHours(2));

        assertAll(
                () -> assertEquals(window(2, 6), plan.activityOf(first.id()).window()),
                () -> assertEquals(window(6, 8), plan.activityOf(second.id()).window()),
                () -> assertEquals(window(8, 11), plan.activityOf(third.id()).window())
        );
    }

    @Test
    void delayOutsidePeriodLeavesWindowsUnchanged() {
        PlanContent plan = new PlanContent(charterFor(window(0, 10)));
        Activity first = sampling();
        Activity second = transit();
        plan.addActivity(first);
        plan.addActivity(second);
        plan.addDependency(second.id(), first.id());

        assertThrows(InvalidItinerary.class, () -> plan.delay(first.id(), Duration.ofHours(6)));
        assertAll(
                () -> assertEquals(window(0, 4), plan.activityOf(first.id()).window()),
                () -> assertEquals(window(4, 6), plan.activityOf(second.id()).window())
        );
    }

    @Test
    void rejectsAssignmentToUnknownActivity() {
        PlanContent plan = wetlandPlan();

        assertThrows(
                InvalidItinerary.class,
                () -> plan.addAssignment(new PersonAssignment(new ActivityId(UUID.randomUUID()), new PersonId(UUID.randomUUID())))
        );
    }

    @Test
    void rejectsDuplicateAssignment() {
        PlanContent plan = wetlandPlan();
        Activity activity = sampling();
        plan.addActivity(activity);
        PersonAssignment assignment = new PersonAssignment(activity.id(), new PersonId(UUID.randomUUID()));
        plan.addAssignment(assignment);

        assertThrows(InvalidAssignment.class, () -> plan.addAssignment(assignment));
    }

    @Test
    void attachesAPermit() {
        PlanContent plan = wetlandPlan();
        PermitId permit = new PermitId(UUID.randomUUID());

        plan.addPermit(permit);

        assertEquals(List.of(permit), plan.permits());
    }

    @Test
    void rejectsDuplicatePermit() {
        PlanContent plan = wetlandPlan();
        PermitId permit = new PermitId(UUID.randomUUID());
        plan.addPermit(permit);

        assertThrows(InvalidValue.class, () -> plan.addPermit(permit));
    }

    private static PlanContent wetlandPlan() {
        return new PlanContent(charterFor(new TimePeriod(DAY, DAY.plus(Duration.ofDays(5)))));
    }

    private static ExpeditionCharter charterFor(TimePeriod period) {
        return new ExpeditionCharter(
                List.of(new Objective("Map wetland biodiversity")),
                period,
                List.of(DELTA),
                List.of(new PersonId(UUID.randomUUID())),
                List.of(new Restriction("No night work"))
        );
    }

    private static Activity sampling() {
        return Activity.sampling(new CertificationId(UUID.randomUUID()))
                .named(new ActivityId(UUID.randomUUID()), "Soil sampling")
                .estimated(Duration.ofHours(4), RiskLevel.MEDIUM)
                .in(DELTA, window(0, 4))
                .build();
    }

    private static Activity transit() {
        return transit("Camp to site", 4, 6, DELTA);
    }

    private static Activity transit(String name, int fromHour, int toHour, WorkZone zone) {
        return Activity.transit()
                .named(new ActivityId(UUID.randomUUID()), name)
                .estimated(Duration.ofHours(toHour - fromHour), RiskLevel.LOW)
                .in(zone, window(fromHour, toHour))
                .build();
    }

    private static Activity measurement() {
        return Activity.measurement(new CertificationId(UUID.randomUUID()), new InstrumentKind("probe"))
                .named(new ActivityId(UUID.randomUUID()), "Water measurement")
                .estimated(Duration.ofHours(3), RiskLevel.HIGH)
                .in(DELTA, window(6, 9))
                .build();
    }

    private static TimePeriod window(int fromHour, int toHour) {
        return new TimePeriod(DAY.plus(Duration.ofHours(fromHour)), DAY.plus(Duration.ofHours(toHour)));
    }

    private static List<ActivityId> activityIds(PlanContent plan) {
        return plan.activities().stream().map(Activity::id).toList();
    }
}
