package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.assessment.IssueSeverity;
import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.ItineraryItem;
import edu.itba.fieldops.domain.shared.DomainException;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

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

    @Test
    void aRestoredPlanHasTheStateItWasStoredWith() {
        Expedition expedition = wetlandDraft();
        expedition.addActivity(sampling());
        expedition.submitForReview();
        expedition.acceptWarning(acceptedCapacityWarning(expedition));
        expedition.markApproved();

        Expedition restored = Expedition.restore(expedition.state());

        assertAll(
                () -> assertEquals(expedition.state(), restored.state()),
                () -> assertTrue(restored.hasAccepted(acceptedCapacityWarning(expedition).issue()))
        );
    }

    @Test
    void aRestoredRevisionKeepsItsLineageAndItsLifecycle() {
        Expedition approved = approvedWithSampling();
        Expedition revision = approved.reviseAsDraft(new ExpeditionId(UUID.randomUUID()));

        Expedition restoredRevision = Expedition.restore(revision.state());
        Expedition restoredApproved = Expedition.restore(approved.state());
        restoredRevision.addActivity(transit());
        restoredApproved.markSuperseded();

        assertAll(
                () -> assertEquals(2, restoredRevision.version()),
                () -> assertEquals(Optional.of(approved.id()), restoredRevision.supersedes()),
                () -> assertEquals(2, restoredRevision.activities().size()),
                () -> assertEquals(ExpeditionStatus.SUPERSEDED, restoredApproved.status())
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("statesThatBreakAnInvariant")
    void rejectsRestoringAStateThatBreaksAnInvariant(String broken, Supplier<ExpeditionState> state) {
        assertThrows(DomainException.class, () -> Expedition.restore(state.get()));
    }

    private static Stream<Arguments> statesThatBreakAnInvariant() {
        ExpeditionState approved = approvedWithSampling().state();
        ExpeditionState draft = wetlandDraft().state();
        AcceptedWarning byStranger = new AcceptedWarning(
                new ValidationIssue(IssueSeverity.WARNING, "CAPACITY", "vehicle near capacity"),
                "extra trailer available",
                new PersonId(UUID.randomUUID())
        );
        Activity elsewhere = Activity.transit()
                .named(new ActivityId(UUID.randomUUID()), "Elsewhere")
                .estimated(Duration.ofHours(2), RiskLevel.LOW)
                .in(new WorkZone("Andes"), window(0, 2))
                .build();
        return Stream.of(
                arguments("a first version that supersedes a plan", supplied(() -> withLineage(
                        approved, 1, Optional.of(new ExpeditionId(UUID.randomUUID()))
                ))),
                arguments("a revision that supersedes nothing", supplied(() -> withLineage(approved, 2, Optional.empty()))),
                arguments("a draft with accepted warnings", supplied(() -> withWarnings(
                        withStatus(approved, ExpeditionStatus.DRAFT),
                        List.of(acceptedCapacityWarning(Expedition.restore(approved)))
                ))),
                arguments("a plan in review without activities", supplied(() -> withStatus(draft, ExpeditionStatus.IN_REVIEW))),
                arguments("a warning accepted by someone who is not responsible", supplied(() -> withWarnings(
                        approved, List.of(byStranger)
                ))),
                arguments("an activity outside the expedition zones", supplied(() -> withItems(draft, List.of(elsewhere)))),
                arguments("an assignment to an unknown activity", supplied(() -> withAssignments(draft, List.of(
                        new PersonAssignment(new ActivityId(UUID.randomUUID()), new PersonId(UUID.randomUUID()))
                ))))
        );
    }

    private static Supplier<ExpeditionState> supplied(Supplier<ExpeditionState> state) {
        return state;
    }

    private static ExpeditionState withLineage(ExpeditionState state, int version, Optional<ExpeditionId> supersedes) {
        return new ExpeditionState(
                state.id(), version, supersedes, state.status(), state.charter(),
                state.items(), state.assignments(), state.permits(), state.acceptedWarnings()
        );
    }

    private static ExpeditionState withStatus(ExpeditionState state, ExpeditionStatus status) {
        return new ExpeditionState(
                state.id(), state.version(), state.supersedes(), status, state.charter(),
                state.items(), state.assignments(), state.permits(), state.acceptedWarnings()
        );
    }

    private static ExpeditionState withItems(ExpeditionState state, List<ItineraryItem> items) {
        return new ExpeditionState(
                state.id(), state.version(), state.supersedes(), state.status(), state.charter(),
                items, state.assignments(), state.permits(), state.acceptedWarnings()
        );
    }

    private static ExpeditionState withAssignments(ExpeditionState state, List<Assignment> assignments) {
        return new ExpeditionState(
                state.id(), state.version(), state.supersedes(), state.status(), state.charter(),
                state.items(), assignments, state.permits(), state.acceptedWarnings()
        );
    }

    private static ExpeditionState withWarnings(ExpeditionState state, List<AcceptedWarning> warnings) {
        return new ExpeditionState(
                state.id(), state.version(), state.supersedes(), state.status(), state.charter(),
                state.items(), state.assignments(), state.permits(), warnings
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
