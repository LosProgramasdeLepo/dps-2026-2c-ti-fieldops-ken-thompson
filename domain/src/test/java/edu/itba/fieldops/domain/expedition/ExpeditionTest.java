package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Certification;
import edu.itba.fieldops.domain.catalog.Permit;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.catalog.ResourceCatalog;
import edu.itba.fieldops.domain.catalog.Vehicle;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.InvalidItinerary;
import edu.itba.fieldops.domain.shared.InvalidExpeditionTransition;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.Passengers;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.tracking.ExpeditionExecution;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.domain.tracking.InvalidActivityExecution;
import edu.itba.fieldops.domain.tracking.Observation;
import edu.itba.fieldops.domain.validation.ExpeditionValidator;
import edu.itba.fieldops.domain.assessment.IssueSeverity;
import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.assessment.ValidationResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpeditionTest {
    private final ExpeditionLifecycle lifecycle = new ExpeditionLifecycle();
    private final ApproveExpedition approve = new ApproveExpedition(ExpeditionValidator.withDefaultRules());

    private static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");
    private static final WorkZone DELTA = new WorkZone("Delta");

    @Test
    void draftHoldsObjectivesPeriodZonesAndRestrictions() {
        List<Objective> objectives = List.of(new Objective("Map wetland biodiversity"));
        TimePeriod period = new TimePeriod(DAY, DAY.plusSeconds(86_400 * 5));
        List<PersonId> responsibles = List.of(new PersonId(UUID.randomUUID()));
        List<Restriction> restrictions = List.of(new Restriction("No night work"));

        Expedition expedition = Expedition.draft(
                new ExpeditionId(UUID.randomUUID()),
                objectives,
                period,
                List.of(DELTA),
                responsibles,
                restrictions
        );

        assertAll(
                () -> assertEquals(ExpeditionStatus.DRAFT, expedition.status()),
                () -> assertEquals(objectives, expedition.objectives()),
                () -> assertEquals(period, expedition.period()),
                () -> assertEquals(List.of(DELTA), expedition.zones()),
                () -> assertEquals(responsibles, expedition.responsibles()),
                () -> assertEquals(restrictions, expedition.restrictions())
        );
    }

    @Test
    void draftAcceptsOneActivityPerKind() {
        Expedition expedition = wetlandDraft();

        expedition.addActivity(sampling());
        expedition.addActivity(transit());
        expedition.addActivity(measurement());

        assertEquals(
                List.of("Soil sampling", "Camp to site", "Water measurement"),
                expedition.itinerary().stream().map(Activity::name).toList()
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
        Activity orphan = Activity.transit(
                new ActivityId(UUID.randomUUID()),
                "dependent",
                Duration.ofHours(2),
                RiskLevel.LOW,
                window(0, 2),
                Set.of(new ActivityId(UUID.randomUUID())),
                DELTA
        );

        assertThrows(InvalidItinerary.class, () -> expedition.addActivity(orphan));
    }

    @Test
    void reordersItineraryInDraft() {
        Expedition expedition = wetlandDraft();
        Activity first = sampling();
        Activity second = transit();
        Activity third = measurement();
        expedition.addActivity(first);
        expedition.addActivity(second);
        expedition.addActivity(third);

        expedition.reorderActivities(List.of(third.id(), first.id(), second.id()));

        assertEquals(List.of(third.id(), first.id(), second.id()), activityIds(expedition));
    }

    @Test
    void rejectsReorderThatDropsOrRepeatsActivities() {
        Expedition expedition = wetlandDraft();
        Activity first = sampling();
        Activity second = transit();
        expedition.addActivity(first);
        expedition.addActivity(second);

        assertAll(
                () -> assertThrows(
                        InvalidItinerary.class,
                        () -> expedition.reorderActivities(List.of(first.id(), first.id()))
                ),
                () -> assertThrows(
                        InvalidItinerary.class,
                        () -> expedition.reorderActivities(List.of(first.id()))
                )
        );
    }

    @Test
    void rejectsReorderOutsideDraft() {
        Expedition expedition = wetlandDraft();
        Activity activity = sampling();
        expedition.addActivity(activity);
        expedition.submitForReview();

        assertThrows(
                InvalidExpeditionTransition.class,
                () -> expedition.reorderActivities(List.of(activity.id()))
        );
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

        assertAll(
                () -> assertThrows(InvalidItinerary.class, () -> expedition.addDependency(overlapping.id(), first.id())),
                () -> assertThrows(InvalidItinerary.class, () -> expedition.addActivity(Activity.transit(
                        new ActivityId(UUID.randomUUID()),
                        "late start",
                        Duration.ofHours(2),
                        RiskLevel.LOW,
                        window(3, 5),
                        Set.of(first.id()),
                        DELTA
                )))
        );
    }

    @Test
    void cannotStartDependentBeforePredecessorFinishes() {
        Expedition expedition = approvedWithTwoDependentActivities();
        Activity first = expedition.itinerary().getFirst();
        Activity second = expedition.itinerary().getLast();
        ExpeditionExecution execution = lifecycle.start(expedition, null);

        assertThrows(
                InvalidActivityExecution.class,
                () -> lifecycle.startActivity(expedition, execution, second.id(), DAY.plusSeconds(4 * 3600L))
        );

        lifecycle.startActivity(expedition, execution, first.id(), DAY);
        assertThrows(
                InvalidActivityExecution.class,
                () -> lifecycle.startActivity(expedition, execution, second.id(), DAY.plusSeconds(4 * 3600L))
        );
    }

    @Test
    void startsDependentAfterPredecessorFinishes() {
        Expedition expedition = approvedWithTwoDependentActivities();
        Activity first = expedition.itinerary().getFirst();
        Activity second = expedition.itinerary().getLast();
        ExpeditionExecution execution = lifecycle.start(expedition, null);
        lifecycle.startActivity(expedition, execution, first.id(), DAY);
        execution.finishActivity(first.id(), DAY.plusSeconds(4 * 3600L), "site reached");

        lifecycle.startActivity(expedition, execution, second.id(), DAY.plusSeconds(4 * 3600L));

        assertEquals(2, execution.executions().size());
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
    void simpleTransitionsReachFinished() {
        Expedition expedition = approvedWithActivity();
        Activity activity = expedition.itinerary().getFirst();
        ExpeditionExecution execution = lifecycle.start(expedition, null);
        lifecycle.startActivity(expedition, execution, activity.id(), DAY);
        execution.finishActivity(activity.id(), DAY.plusSeconds(3600), "samples stored");

        lifecycle.finish(expedition, execution);

        assertAll(
                () -> assertEquals(ExpeditionStatus.APPROVED, expedition.status()),
                () -> assertEquals(ExpeditionExecution.Status.FINISHED, execution.status())
        );
    }

    @Test
    void cannotFinishWhileActivitiesRemainOpen() {
        Expedition expedition = approvedWithActivity();
        Activity activity = expedition.itinerary().getFirst();
        ExpeditionExecution execution = lifecycle.start(expedition, null);

        assertThrows(InvalidActivityExecution.class, () -> lifecycle.finish(expedition, execution));

        lifecycle.startActivity(expedition, execution, activity.id(), DAY);
        assertThrows(InvalidActivityExecution.class, () -> lifecycle.finish(expedition, execution));
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
    void rejectsDuplicatePermit() {
        Expedition expedition = wetlandDraft();
        PermitId permit = new PermitId(UUID.randomUUID());
        expedition.addPermit(permit);

        assertThrows(InvalidValue.class, () -> expedition.addPermit(permit));
    }

    @Test
    void cannotAssignAfterApproval() {
        Expedition expedition = approvedWithActivity();
        Activity activity = expedition.itinerary().getFirst();

        assertThrows(
                InvalidExpeditionTransition.class,
                () -> expedition.addAssignment(new PersonAssignment(activity.id(), new PersonId(UUID.randomUUID())))
        );
    }

    @Test
    void rejectsDuplicateAcceptedWarning() {
        Expedition expedition = wetlandDraft();
        expedition.submitForReview();
        AcceptedWarning warning = acceptedCapacityWarning();
        expedition.acceptWarning(warning);

        assertThrows(InvalidValue.class, () -> expedition.acceptWarning(warning));
    }

    @Test
    void returnToDraftClearsAcceptedWarnings() {
        Expedition expedition = wetlandDraft();
        expedition.submitForReview();
        expedition.acceptWarning(acceptedCapacityWarning());

        expedition.returnToDraft();

        assertTrue(expedition.acceptedWarnings().isEmpty());
    }

    @Test
    void cannotApproveFromDraft() {
        Expedition expedition = wetlandDraft();

        assertThrows(
                InvalidExpeditionTransition.class,
                () -> approve.approve(expedition, new ResourceCatalog(), OccupyingExpeditions.none())
        );
        assertEquals(ExpeditionStatus.DRAFT, expedition.status());
    }

    @Test
    void cannotApproveWithCriticalIssues() {
        Expedition expedition = wetlandDraft();
        expedition.addActivity(sampling());
        expedition.submitForReview();

        assertThrows(
                ExpeditionNotApprovable.class,
                () -> approve.approve(expedition, new ResourceCatalog(), OccupyingExpeditions.none())
        );
        assertEquals(ExpeditionStatus.IN_REVIEW, expedition.status());
    }

    @Test
    void approveRevalidatesAfterAnInvalidAssignment() {
        Prepared sampling = preparedSampling();
        sampling.expedition().submitForReview();
        ValidationResult clean = ExpeditionValidator.withDefaultRules()
                .validate(sampling.expedition(), sampling.catalog(), OccupyingExpeditions.none());
        assertTrue(clean.issues().isEmpty());
        sampling.expedition().addAssignment(new PersonAssignment(
                sampling.activity().id(),
                new PersonId(UUID.randomUUID())
        ));

        assertThrows(
                ExpeditionNotApprovable.class,
                () -> approve.approve(sampling.expedition(), sampling.catalog(), OccupyingExpeditions.none())
        );
        assertEquals(ExpeditionStatus.IN_REVIEW, sampling.expedition().status());
    }

    @Test
    void cannotApproveWarningWithoutJustification() {
        Prepared crowded = crowdedTransit();
        crowded.expedition().submitForReview();

        assertThrows(
                ExpeditionNotApprovable.class,
                () -> approve.approve(crowded.expedition(), crowded.catalog(), OccupyingExpeditions.none())
        );
    }

    @Test
    void approvesWhenWarningIsJustified() {
        Prepared crowded = crowdedTransit();
        crowded.expedition().submitForReview();
        ValidationResult result = ExpeditionValidator.withDefaultRules()
                .validate(crowded.expedition(), crowded.catalog(), OccupyingExpeditions.none());
        result.warnings().forEach(warning -> crowded.expedition().acceptWarning(
                new AcceptedWarning(warning, "backup team on site", new PersonId(UUID.randomUUID()))
        ));

        approve.approve(crowded.expedition(), crowded.catalog(), OccupyingExpeditions.none());

        assertEquals(ExpeditionStatus.APPROVED, crowded.expedition().status());
    }

    @Test
    void suspendsFromApproved() {
        Expedition expedition = approvedWithActivity();

        ExpeditionExecution execution = lifecycle.suspend(expedition, null);

        assertAll(
                () -> assertEquals(ExpeditionStatus.APPROVED, expedition.status()),
                () -> assertEquals(ExpeditionExecution.Status.SUSPENDED, execution.status())
        );
    }

    @Test
    void resumeReturnsToInProgress() {
        Expedition expedition = approvedWithActivity();
        ExpeditionExecution execution = lifecycle.start(expedition, null);
        lifecycle.suspend(expedition, execution);

        execution.resume();

        assertEquals(ExpeditionExecution.Status.IN_PROGRESS, execution.status());
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

        assertAll(
                () -> assertEquals(1, expedition.itinerary().size()),
                () -> assertEquals("Camp to site", expedition.itinerary().getFirst().name())
        );
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
    void returnToDraftFromApprovedClearsTheApproval() {
        Expedition expedition = approvedWithActivity();

        lifecycle.returnToDraft(expedition, null);

        assertEquals(ExpeditionStatus.DRAFT, expedition.status());
    }

    @Test
    void returnToDraftFromInProgressClearsExecutions() {
        Expedition expedition = approvedWithActivity();
        Activity activity = expedition.itinerary().getFirst();
        ExpeditionExecution execution = lifecycle.start(expedition, null);
        lifecycle.startActivity(expedition, execution, activity.id(), DAY);

        lifecycle.returnToDraft(expedition, execution);

        assertAll(
                () -> assertEquals(ExpeditionStatus.DRAFT, expedition.status()),
                () -> assertTrue(execution.executions().isEmpty())
        );
    }

    @Test
    void cannotReturnToDraftFromFinished() {
        Expedition expedition = approvedWithActivity();
        Activity activity = expedition.itinerary().getFirst();
        ExpeditionExecution execution = lifecycle.start(expedition, null);
        lifecycle.startActivity(expedition, execution, activity.id(), DAY);
        execution.finishActivity(activity.id(), DAY.plusSeconds(3600), "samples stored");
        lifecycle.finish(expedition, execution);

        assertThrows(InvalidExpeditionTransition.class, () -> lifecycle.returnToDraft(expedition, execution));
        assertEquals(ExpeditionExecution.Status.FINISHED, execution.status());
    }

    @Test
    void lifecycleRejectsAnExecutionFromAnotherExpedition() {
        Expedition first = approvedWithActivity();
        Expedition second = approvedWithActivity();
        ExpeditionExecution execution = lifecycle.start(first, null);

        assertThrows(InvalidValue.class, () -> lifecycle.finish(second, execution));
        assertEquals(ExpeditionExecution.Status.IN_PROGRESS, execution.status());
    }

    @Test
    void delayShiftsDependentActivityWindows() {
        Expedition expedition = wetlandDraft();
        Activity first = sampling();
        Activity second = transit();
        expedition.addActivity(first);
        expedition.addActivity(second);
        expedition.addDependency(second.id(), first.id());

        expedition.delay(first.id(), Duration.ofHours(2));

        assertAll(
                () -> assertEquals(window(2, 6), expedition.activityOf(first.id()).window()),
                () -> assertEquals(window(6, 8), expedition.activityOf(second.id()).window())
        );
    }

    @Test
    void delayShiftsDependentsRegardlessOfItineraryOrder() {
        Expedition expedition = wetlandDraft();
        Activity first = sampling();
        Activity second = transit();
        Activity third = measurement();
        expedition.addActivity(first);
        expedition.addActivity(second);
        expedition.addActivity(third);
        expedition.addDependency(second.id(), first.id());
        expedition.addDependency(third.id(), second.id());
        expedition.reorderActivities(List.of(third.id(), second.id(), first.id()));

        expedition.delay(first.id(), Duration.ofHours(2));

        assertAll(
                () -> assertEquals(window(2, 6), expedition.activityOf(first.id()).window()),
                () -> assertEquals(window(6, 8), expedition.activityOf(second.id()).window()),
                () -> assertEquals(window(8, 11), expedition.activityOf(third.id()).window())
        );
    }

    @Test
    void delayOutsidePeriodLeavesWindowsUnchanged() {
        Expedition expedition = Expedition.draft(
                new ExpeditionId(UUID.randomUUID()),
                List.of(new Objective("Map wetland biodiversity")),
                window(0, 10),
                List.of(DELTA),
                List.of(new PersonId(UUID.randomUUID())),
                List.of(new Restriction("No night work"))
        );
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
    void attachesPermitInDraft() {
        Expedition expedition = wetlandDraft();
        PermitId permit = new PermitId(UUID.randomUUID());

        expedition.addPermit(permit);

        assertEquals(List.of(permit), expedition.permits());
    }

    @Test
    void recordsObservationWhileInProgress() {
        Expedition expedition = approvedWithActivity();
        ExpeditionExecution execution = lifecycle.start(expedition, null);
        Observation observation = new Observation("site wet", DAY);

        execution.addObservation(observation);

        assertEquals(List.of(observation), execution.observations());
    }

    @Test
    void recordsIncidentWhileInProgress() {
        Expedition expedition = approvedWithActivity();
        ExpeditionExecution execution = lifecycle.start(expedition, null);
        Incident incident = Incident.of("rain delay", DAY);

        execution.addIncident(incident);

        assertEquals(List.of(incident), execution.incidents());
    }

    @Test
    void tracksActivityExecution() {
        Expedition expedition = approvedWithActivity();
        Activity activity = expedition.itinerary().getFirst();
        ExpeditionExecution execution = lifecycle.start(expedition, null);
        lifecycle.startActivity(expedition, execution, activity.id(), DAY);

        execution.finishActivity(activity.id(), DAY.plusSeconds(3600), "samples stored");

        assertAll(
                () -> assertEquals(1, execution.executions().size()),
                () -> assertTrue(execution.executions().getFirst().isFinished())
        );
    }

    @Test
    void cannotFinishActivityTwice() {
        Expedition expedition = approvedWithActivity();
        Activity activity = expedition.itinerary().getFirst();
        ExpeditionExecution execution = lifecycle.start(expedition, null);
        lifecycle.startActivity(expedition, execution, activity.id(), DAY);
        execution.finishActivity(activity.id(), DAY.plusSeconds(3600), "samples stored");

        assertThrows(
                InvalidActivityExecution.class,
                () -> execution.finishActivity(activity.id(), DAY.plusSeconds(7200), "samples stored again")
        );
    }

    @Test
    void finishingAReturnedExecutionDoesNotChangeTheExpedition() {
        Expedition expedition = approvedWithActivity();
        Activity activity = expedition.itinerary().getFirst();
        ExpeditionExecution execution = lifecycle.start(expedition, null);
        lifecycle.startActivity(expedition, execution, activity.id(), DAY);

        execution.executions().getFirst().finish(DAY.plusSeconds(3600), "samples stored");

        assertFalse(execution.executions().getFirst().isFinished());
    }

    private Expedition approvedWithActivity() {
        Prepared prepared = preparedSampling();
        prepared.expedition().submitForReview();
        approve.approve(prepared.expedition(), prepared.catalog(), OccupyingExpeditions.none());
        return prepared.expedition();
    }

    private Expedition approvedWithTwoDependentActivities() {
        Activity first = sampling();
        Activity second = transit();
        Expedition expedition = wetlandDraft();
        expedition.addActivity(first);
        expedition.addActivity(second);
        expedition.addDependency(second.id(), first.id());
        PersonId personId = new PersonId(UUID.randomUUID());
        VehicleId vehicleId = new VehicleId(UUID.randomUUID());
        expedition.addAssignment(new PersonAssignment(first.id(), personId));
        expedition.addAssignment(new VehicleAssignment(second.id(), vehicleId));
        Permit firstPermit = permitFor(first);
        Permit secondPermit = permitFor(second);
        expedition.addPermit(firstPermit.id());
        expedition.addPermit(secondPermit.id());
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(person(personId, certificationOf(first)));
        catalog.add(new Vehicle(vehicleId, new Passengers(2), Availability.always()));
        catalog.add(firstPermit);
        catalog.add(secondPermit);
        expedition.submitForReview();
        approve.approve(expedition, catalog, OccupyingExpeditions.none());
        return expedition;
    }

    private static Prepared preparedSampling() {
        Activity activity = sampling();
        PersonId personId = new PersonId(UUID.randomUUID());
        Expedition expedition = wetlandDraft();
        expedition.addActivity(activity);
        expedition.addAssignment(new PersonAssignment(activity.id(), personId));
        Permit permit = permitFor(activity);
        expedition.addPermit(permit.id());
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(person(personId, certificationOf(activity)));
        catalog.add(permit);
        return new Prepared(expedition, catalog, activity);
    }

    private static Prepared crowdedTransit() {
        Activity activity = transit();
        PersonId ada = new PersonId(UUID.randomUUID());
        PersonId bob = new PersonId(UUID.randomUUID());
        VehicleId vehicleId = new VehicleId(UUID.randomUUID());
        Expedition expedition = wetlandDraft();
        expedition.addActivity(activity);
        expedition.addAssignment(new PersonAssignment(activity.id(), ada));
        expedition.addAssignment(new PersonAssignment(activity.id(), bob));
        expedition.addAssignment(new VehicleAssignment(activity.id(), vehicleId));
        Permit permit = permitFor(activity);
        expedition.addPermit(permit.id());
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(new Person(ada, "Ada", List.of(), Availability.always()));
        catalog.add(new Person(bob, "Bob", List.of(), Availability.always()));
        catalog.add(new Vehicle(vehicleId, new Passengers(1), Availability.always()));
        catalog.add(permit);
        return new Prepared(expedition, catalog, activity);
    }

    private static Person person(PersonId personId, CertificationId certificationId) {
        return new Person(
                personId,
                "Ada",
                List.of(new Certification(certificationId, "Sampling")),
                Availability.always()
        );
    }

    private static CertificationId certificationOf(Activity activity) {
        return activity.requirements().certifications().iterator().next();
    }

    private static Permit permitFor(Activity activity) {
        return new Permit(new PermitId(UUID.randomUUID()), activity.zone(), activity.window());
    }

    private static Expedition wetlandDraft() {
        return Expedition.draft(
                new ExpeditionId(UUID.randomUUID()),
                List.of(new Objective("Map wetland biodiversity")),
                new TimePeriod(DAY, DAY.plusSeconds(86_400 * 5)),
                List.of(DELTA),
                List.of(new PersonId(UUID.randomUUID())),
                List.of(new Restriction("No night work"))
        );
    }

    private static AcceptedWarning acceptedCapacityWarning() {
        ValidationIssue issue = new ValidationIssue(IssueSeverity.WARNING, "CAPACITY", "vehicle near capacity");
        return new AcceptedWarning(issue, "extra trailer available", new PersonId(UUID.randomUUID()));
    }

    private static Activity sampling() {
        return Activity.sampling(
                new ActivityId(UUID.randomUUID()),
                "Soil sampling",
                Duration.ofHours(4),
                RiskLevel.MEDIUM,
                window(0, 4),
                Set.of(),
                DELTA,
                new CertificationId(UUID.randomUUID())
        );
    }

    private static Activity transit() {
        return transit("Camp to site", 4, 6, DELTA);
    }

    private static Activity transit(String name, int fromHour, int toHour, WorkZone zone) {
        return Activity.transit(
                new ActivityId(UUID.randomUUID()),
                name,
                Duration.ofHours(toHour - fromHour),
                RiskLevel.LOW,
                window(fromHour, toHour),
                Set.of(),
                zone
        );
    }

    private static Activity measurement() {
        return Activity.measurement(
                new ActivityId(UUID.randomUUID()),
                "Water measurement",
                Duration.ofHours(3),
                RiskLevel.HIGH,
                window(6, 9),
                Set.of(),
                DELTA,
                new CertificationId(UUID.randomUUID()),
                new edu.itba.fieldops.domain.shared.InstrumentKind("probe")
        );
    }

    private static TimePeriod window(int fromHour, int toHour) {
        return new TimePeriod(DAY.plusSeconds(fromHour * 3600L), DAY.plusSeconds(toHour * 3600L));
    }

    private static List<ActivityId> activityIds(Expedition expedition) {
        return expedition.itinerary().stream().map(Activity::id).toList();
    }

    private record Prepared(Expedition expedition, ResourceCatalog catalog, Activity activity) {
    }
}
