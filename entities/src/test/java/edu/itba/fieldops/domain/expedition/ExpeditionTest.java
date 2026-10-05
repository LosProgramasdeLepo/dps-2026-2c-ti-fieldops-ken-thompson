package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.assessment.IssueSeverity;
import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpeditionTest {
    private static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");
    private static final WorkZone DELTA = new WorkZone("Delta");

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
        return Expedition.draft(
                new ExpeditionId(UUID.randomUUID()),
                new ExpeditionCharter(
                        List.of(new Objective("Map wetland biodiversity")),
                        new TimePeriod(DAY, DAY.plus(Duration.ofDays(5))),
                        List.of(DELTA),
                        List.of(new PersonId(UUID.randomUUID())),
                        List.of(new Restriction("No night work"))
                )
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
        return Activity.transit()
                .named(new ActivityId(UUID.randomUUID()), "Camp to site")
                .estimated(Duration.ofHours(2), RiskLevel.LOW)
                .in(DELTA, window(4, 6))
                .build();
    }

    private static TimePeriod window(int fromHour, int toHour) {
        return new TimePeriod(DAY.plus(Duration.ofHours(fromHour)), DAY.plus(Duration.ofHours(toHour)));
    }

    private static List<ActivityId> activityIds(Expedition expedition) {
        return expedition.activities().stream().map(Activity::id).toList();
    }
}
