package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.assessment.IssueSeverity;
import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.InvalidItinerary;
import edu.itba.fieldops.domain.shared.InstrumentKind;
import edu.itba.fieldops.domain.shared.InvalidExpeditionTransition;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.Stock;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpeditionTest {
    private static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");
    private static final WorkZone DELTA = new WorkZone("Delta");

    @Test
    void draftAcceptsOneActivityPerKind() {
        Expedition expedition = wetlandDraft();

        expedition.addActivity(sampling());
        expedition.addActivity(transit());
        expedition.addActivity(measurement());

        assertEquals(
                List.of("Soil sampling", "Camp to site", "Water measurement"),
                expedition.activities().stream().map(Activity::name).toList()
        );
    }

    @Test
    void rejectsActivityInUnknownZone() {
        Expedition expedition = wetlandDraft();
        Activity coast = transit("coast sample", 0, 2, new WorkZone("Coast"));

        assertThrows(InvalidItinerary.class, () -> expedition.addActivity(coast));
    }

    @Test
    void rejectsActivityOutsidePeriod() {
        Expedition expedition = wetlandDraft();
        Activity late = transit("late", 200, 203, DELTA);

        assertThrows(InvalidItinerary.class, () -> expedition.addActivity(late));
    }

    @Test
    void rejectsUnknownPredecessor() {
        Expedition expedition = wetlandDraft();
        Activity orphan = Activity.transit()
                .named(new ActivityId(UUID.randomUUID()), "dependent")
                .estimated(Duration.ofHours(2), RiskLevel.LOW)
                .in(DELTA, window(0, 2))
                .after(Set.of(new ActivityId(UUID.randomUUID())))
                .build();

        assertThrows(InvalidItinerary.class, () -> expedition.addActivity(orphan));
    }

    @Test
    void addsDependencyWhenPredecessorFinishesBefore() {
        Expedition expedition = wetlandDraft();
        Activity first = sampling();
        Activity second = transit();
        expedition.addActivity(first);
        expedition.addActivity(second);

        expedition.addDependency(second.id(), first.id());

        assertTrue(expedition.activityOf(second.id()).predecessors().contains(first.id()));
    }

    @Test
    void rejectsCyclicDependency() {
        Expedition expedition = wetlandDraft();
        Activity first = sampling();
        Activity second = transit();
        Activity third = measurement();
        expedition.addActivity(first);
        expedition.addActivity(second);
        expedition.addActivity(third);
        expedition.addDependency(second.id(), first.id());
        expedition.addDependency(third.id(), second.id());

        assertThrows(InvalidItinerary.class, () -> expedition.addDependency(first.id(), third.id()));
    }

    @Test
    void rejectsPredecessorThatDoesNotFinishBefore() {
        Expedition expedition = wetlandDraft();
        Activity first = sampling();
        Activity overlapping = transit("overlap", 3, 5, DELTA);
        expedition.addActivity(first);
        expedition.addActivity(overlapping);

        assertThrows(InvalidItinerary.class, () -> expedition.addDependency(overlapping.id(), first.id()));
    }

    @Test
    void rejectsAnActivityThatStartsBeforeItsPredecessorFinishes() {
        Expedition expedition = wetlandDraft();
        Activity first = sampling();
        expedition.addActivity(first);
        Activity lateStart = Activity.transit()
                .named(new ActivityId(UUID.randomUUID()), "late start")
                .estimated(Duration.ofHours(2), RiskLevel.LOW)
                .in(DELTA, window(3, 5))
                .after(Set.of(first.id()))
                .build();

        assertThrows(InvalidItinerary.class, () -> expedition.addActivity(lateStart));
    }

    @Test
    void removingPredecessorDropsTheDependency() {
        Expedition expedition = wetlandDraft();
        Activity first = sampling();
        Activity second = transit();
        expedition.addActivity(first);
        expedition.addActivity(second);
        expedition.addDependency(second.id(), first.id());

        expedition.removeActivity(first.id());

        assertAll(
                () -> assertEquals(List.of(second.id()), activityIds(expedition)),
                () -> assertTrue(expedition.activityOf(second.id()).predecessors().isEmpty())
        );
    }

    @Test
    void delayShiftsTheWholeChainOfDependents() {
        Expedition expedition = wetlandDraft();
        Activity first = sampling();
        Activity second = transit();
        Activity third = measurement();
        expedition.addActivity(first);
        expedition.addActivity(second);
        expedition.addActivity(third);
        expedition.addDependency(second.id(), first.id());
        expedition.addDependency(third.id(), second.id());

        expedition.delay(first.id(), Duration.ofHours(2));

        assertAll(
                () -> assertEquals(window(2, 6), expedition.activityOf(first.id()).window()),
                () -> assertEquals(window(6, 8), expedition.activityOf(second.id()).window()),
                () -> assertEquals(window(8, 11), expedition.activityOf(third.id()).window())
        );
    }

    @Test
    void delayOutsidePeriodLeavesWindowsUnchanged() {
        Expedition expedition = Expedition.draft(new ExpeditionId(UUID.randomUUID()), charterFor(window(0, 10)));
        Activity first = sampling();
        Activity second = transit();
        expedition.addActivity(first);
        expedition.addActivity(second);
        expedition.addDependency(second.id(), first.id());

        assertThrows(InvalidItinerary.class, () -> expedition.delay(first.id(), Duration.ofHours(6)));
        assertAll(
                () -> assertEquals(window(0, 4), expedition.activityOf(first.id()).window()),
                () -> assertEquals(window(4, 6), expedition.activityOf(second.id()).window())
        );
    }

    @Test
    void rejectsAssignmentToUnknownActivity() {
        Expedition expedition = wetlandDraft();

        assertThrows(
                InvalidItinerary.class,
                () -> expedition.addAssignment(new PersonAssignment(new ActivityId(UUID.randomUUID()), new PersonId(UUID.randomUUID())))
        );
    }

    @Test
    void rejectsDuplicateAssignment() {
        Expedition expedition = wetlandDraft();
        Activity activity = sampling();
        expedition.addActivity(activity);
        PersonAssignment assignment = new PersonAssignment(activity.id(), new PersonId(UUID.randomUUID()));
        expedition.addAssignment(assignment);

        assertThrows(InvalidAssignment.class, () -> expedition.addAssignment(assignment));
    }

    @Test
    void rejectsAConsumableAssignmentOfNothing() {
        ActivityId activityId = new ActivityId(UUID.randomUUID());
        ConsumableId vials = new ConsumableId(UUID.randomUUID());

        assertThrows(InvalidAssignment.class, () -> new ConsumableAssignment(activityId, vials, new Stock(0)));
    }

    @Test
    void attachesPermitInDraft() {
        Expedition expedition = wetlandDraft();
        PermitId permit = new PermitId(UUID.randomUUID());

        expedition.addPermit(permit);

        assertEquals(List.of(permit), expedition.permits());
    }

    @Test
    void rejectsDuplicatePermit() {
        Expedition expedition = wetlandDraft();
        PermitId permit = new PermitId(UUID.randomUUID());
        expedition.addPermit(permit);

        assertThrows(InvalidValue.class, () -> expedition.addPermit(permit));
    }

    @Test
    void rejectsEditsWhileInReview() {
        Expedition expedition = wetlandDraft();
        Activity activity = sampling();
        expedition.addActivity(activity);
        expedition.submitForReview();

        assertThrows(
                InvalidExpeditionTransition.class,
                () -> expedition.addAssignment(new PersonAssignment(activity.id(), new PersonId(UUID.randomUUID())))
        );
        assertEquals(ExpeditionStatus.IN_REVIEW, expedition.status());
    }

    @Test
    void cannotAssignAfterApproval() {
        Expedition expedition = approvedWithSampling();
        Activity activity = expedition.activities().getFirst();

        assertThrows(
                InvalidExpeditionTransition.class,
                () -> expedition.addAssignment(new PersonAssignment(activity.id(), new PersonId(UUID.randomUUID())))
        );
    }

    @Test
    void rejectsDuplicateAcceptedWarning() {
        Expedition expedition = wetlandDraft();
        expedition.addActivity(sampling());
        expedition.submitForReview();
        AcceptedWarning warning = acceptedCapacityWarning(expedition);
        expedition.acceptWarning(warning);

        assertThrows(InvalidValue.class, () -> expedition.acceptWarning(warning));
    }

    @Test
    void returnToDraftClearsAcceptedWarnings() {
        Expedition expedition = wetlandDraft();
        expedition.addActivity(sampling());
        expedition.submitForReview();
        expedition.acceptWarning(acceptedCapacityWarning(expedition));

        expedition.returnToDraft();

        assertTrue(expedition.acceptedWarnings().isEmpty());
    }

    @Test
    void returnToDraftAllowsChangingItinerary() {
        Expedition expedition = wetlandDraft();
        Activity first = sampling();
        expedition.addActivity(first);
        expedition.submitForReview();
        expedition.returnToDraft();

        expedition.removeActivity(first.id());
        expedition.addActivity(transit());

        assertEquals(List.of("Camp to site"), expedition.activities().stream().map(Activity::name).toList());
    }

    @Test
    void cannotReturnAnApprovedPlanToDraft() {
        Expedition expedition = approvedWithSampling();

        assertThrows(InvalidExpeditionTransition.class, expedition::returnToDraft);

        assertEquals(ExpeditionStatus.APPROVED, expedition.status());
    }

    @Test
    void aRevisionIsTheNextDraftVersionOfTheApprovedPlan() {
        Expedition approved = approvedWithSampling();

        Expedition revision = approved.reviseAsDraft(new ExpeditionId(UUID.randomUUID()));

        assertAll(
                () -> assertEquals(ExpeditionStatus.DRAFT, revision.status()),
                () -> assertEquals(2, revision.version()),
                () -> assertEquals(Optional.of(approved.id()), revision.supersedes()),
                () -> assertEquals(activityIds(approved), activityIds(revision)),
                () -> assertEquals(approved.assignments().all(), revision.assignments().all())
        );
    }

    @Test
    void editingARevisionLeavesTheApprovedPlanUntouched() {
        Expedition approved = approvedWithSampling();
        Activity activity = approved.activities().getFirst();
        Expedition revision = approved.reviseAsDraft(new ExpeditionId(UUID.randomUUID()));

        revision.delay(activity.id(), Duration.ofHours(2));
        revision.removeActivity(activity.id());

        assertAll(
                () -> assertEquals(ExpeditionStatus.APPROVED, approved.status()),
                () -> assertEquals(window(0, 4), approved.activityOf(activity.id()).window()),
                () -> assertEquals(1, approved.assignments().all().size())
        );
    }

    private static Expedition approvedWithSampling() {
        Expedition expedition = wetlandDraft();
        Activity activity = sampling();
        expedition.addActivity(activity);
        expedition.addAssignment(new PersonAssignment(activity.id(), new PersonId(UUID.randomUUID())));
        expedition.submitForReview();
        expedition.markApproved();
        return expedition;
    }

    private static Expedition wetlandDraft() {
        return Expedition.draft(new ExpeditionId(UUID.randomUUID()), charterFor(new TimePeriod(DAY, DAY.plus(Duration.ofDays(5)))));
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

    private static AcceptedWarning acceptedCapacityWarning(Expedition expedition) {
        ValidationIssue issue = new ValidationIssue(IssueSeverity.WARNING, "CAPACITY", "vehicle near capacity");
        return new AcceptedWarning(issue, "extra trailer available", expedition.charter().responsibles().getFirst());
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

    private static List<ActivityId> activityIds(Expedition expedition) {
        return expedition.activities().stream().map(Activity::id).toList();
    }
}
