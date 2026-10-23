package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Permit;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.Delay;
import edu.itba.fieldops.domain.itinerary.EstimatedDuration;
import edu.itba.fieldops.domain.itinerary.InvalidItinerary;
import edu.itba.fieldops.domain.shared.Consumption;
import edu.itba.fieldops.domain.shared.InstrumentKind;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.PermitKind;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.support.ResourceCatalog;
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
    private static final Person ADA = new Person(new PersonId(UUID.randomUUID()), "Ada", List.of(), Availability.always());
    private static final Permit ZONE_PERMIT = new Permit(
            new PermitId(UUID.randomUUID()),
            DELTA,
            new TimePeriod(DAY, DAY.plus(Duration.ofDays(5))),
            PermitKind.ZONE
    );

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
                .estimated(new EstimatedDuration(Duration.ofHours(2)), RiskLevel.LOW)
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
                .estimated(new EstimatedDuration(Duration.ofHours(2)), RiskLevel.LOW)
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

        plan.delay(first.id(), new Delay(Duration.ofHours(2)));

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

        assertThrows(InvalidItinerary.class, () -> plan.delay(first.id(), new Delay(Duration.ofHours(6))));
        assertAll(
                () -> assertEquals(window(0, 4), plan.activityOf(first.id()).window()),
                () -> assertEquals(window(4, 6), plan.activityOf(second.id()).window())
        );
    }

    @Test
    void rejectsAssignmentToUnknownActivity() {
        PlanContent plan = wetlandPlan();
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(ADA);

        assertThrows(
                InvalidItinerary.class,
                () -> plan.addAssignment(new PersonAssignment(new ActivityId(UUID.randomUUID()), ADA.id()), catalog.catalogs())
        );
    }

    @Test
    void rejectsAssigningAPersonTheCatalogDoesNotKnow() {
        PlanContent plan = wetlandPlan();
        Activity activity = sampling();
        plan.addActivity(activity);

        InvalidAssignment rejected = assertThrows(
                InvalidAssignment.class,
                () -> plan.addAssignment(new PersonAssignment(activity.id(), ADA.id()), new ResourceCatalog().catalogs())
        );

        assertEquals("unknown person " + ADA.id().value(), rejected.getMessage());
        assertTrue(plan.assignments().all().isEmpty());
    }

    @Test
    void rejectsAssigningAConsumableTheCatalogDoesNotKnow() {
        PlanContent plan = wetlandPlan();
        Activity activity = sampling();
        plan.addActivity(activity);
        ConsumableAssignment vials = new ConsumableAssignment(activity.id(), new ConsumableId(UUID.randomUUID()), new Consumption(2));

        assertThrows(InvalidAssignment.class, () -> plan.addAssignment(vials, new ResourceCatalog().catalogs()));
        assertTrue(plan.assignments().all().isEmpty());
    }

    @Test
    void rejectsDuplicateAssignment() {
        PlanContent plan = wetlandPlan();
        Activity activity = sampling();
        plan.addActivity(activity);
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(ADA);
        PersonAssignment assignment = new PersonAssignment(activity.id(), ADA.id());
        plan.addAssignment(assignment, catalog.catalogs());

        assertThrows(InvalidAssignment.class, () -> plan.addAssignment(assignment, catalog.catalogs()));
    }

    @Test
    void attachesAPermit() {
        PlanContent plan = wetlandPlan();
        ResourceCatalog catalog = withPermit();

        plan.addPermit(ZONE_PERMIT.id(), catalog);

        assertEquals(List.of(ZONE_PERMIT.id()), plan.permits());
    }

    @Test
    void rejectsAPermitTheCatalogDoesNotKnow() {
        PlanContent plan = wetlandPlan();

        UnknownResource rejected = assertThrows(UnknownResource.class, () -> plan.addPermit(ZONE_PERMIT.id(), new ResourceCatalog()));

        assertEquals("unknown permit: " + ZONE_PERMIT.id().value(), rejected.getMessage());
        assertTrue(plan.permits().isEmpty());
    }

    @Test
    void rejectsDuplicatePermit() {
        PlanContent plan = wetlandPlan();
        ResourceCatalog catalog = withPermit();
        plan.addPermit(ZONE_PERMIT.id(), catalog);

        assertThrows(InvalidValue.class, () -> plan.addPermit(ZONE_PERMIT.id(), catalog));
    }

    private static ResourceCatalog withPermit() {
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(ZONE_PERMIT);
        return catalog;
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
                .estimated(new EstimatedDuration(Duration.ofHours(4)), RiskLevel.MEDIUM)
                .in(DELTA, window(0, 4))
                .build();
    }

    private static Activity transit() {
        return transit("Camp to site", 4, 6, DELTA);
    }

    private static Activity transit(String name, int fromHour, int toHour, WorkZone zone) {
        return Activity.transit()
                .named(new ActivityId(UUID.randomUUID()), name)
                .estimated(new EstimatedDuration(Duration.ofHours(toHour - fromHour)), RiskLevel.LOW)
                .in(zone, window(fromHour, toHour))
                .build();
    }

    private static Activity measurement() {
        return Activity.measurement(new CertificationId(UUID.randomUUID()), new InstrumentKind("probe"))
                .named(new ActivityId(UUID.randomUUID()), "Water measurement")
                .estimated(new EstimatedDuration(Duration.ofHours(3)), RiskLevel.HIGH)
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
