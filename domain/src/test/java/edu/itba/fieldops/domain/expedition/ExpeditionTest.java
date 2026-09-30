package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Certification;
import edu.itba.fieldops.domain.catalog.Permit;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.details.ResourceCatalog;
import edu.itba.fieldops.domain.catalog.Vehicle;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.ActivityBlock;
import edu.itba.fieldops.domain.itinerary.InvalidItinerary;
import edu.itba.fieldops.domain.shared.InvalidExpeditionTransition;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.Passengers;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.Stock;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.tracking.ExpeditionExecution;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.domain.tracking.InvalidActivityExecution;
import edu.itba.fieldops.domain.tracking.Observation;
import edu.itba.fieldops.domain.assessment.IssueSeverity;
import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.assessment.ValidationResult;
import edu.itba.fieldops.domain.validation.RuleBasedValidator;
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
    private static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");
    private static final WorkZone DELTA = new WorkZone("Delta");

    @Test
    void draftHoldsItsCharter() {
        ExpeditionCharter charter = new ExpeditionCharter(
                List.of(new Objective("Map wetland biodiversity")),
                new TimePeriod(DAY, DAY.plusSeconds(86_400 * 5)),
                List.of(DELTA),
                List.of(new PersonId(UUID.randomUUID())),
                List.of(new Restriction("No night work"))
        );

        Expedition expedition = Expedition.draft(new ExpeditionId(UUID.randomUUID()), charter);

        assertAll(
                () -> assertEquals(ExpeditionStatus.DRAFT, expedition.status()),
                () -> assertEquals(charter, expedition.charter())
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

        assertAll(
                () -> assertThrows(InvalidItinerary.class, () -> expedition.addDependency(overlapping.id(), first.id())),
                () -> assertThrows(InvalidItinerary.class, () -> expedition.addActivity(Activity.transit()
                        .named(new ActivityId(UUID.randomUUID()), "late start")
                        .estimated(Duration.ofHours(2), RiskLevel.LOW)
                        .in(DELTA, window(3, 5))
                        .after(Set.of(first.id()))
                        .build()))
        );
    }

    @Test
    void cannotStartDependentBeforePredecessorFinishes() {
        Expedition expedition = approvedWithTwoDependentActivities();
        Activity first = expedition.activities().getFirst();
        Activity second = expedition.activities().getLast();
        ExpeditionExecution execution = ExpeditionExecution.started(expedition.id());

        assertThrows(
                InvalidActivityExecution.class,
                () -> execution.startActivity(second.id(), DAY.plusSeconds(4 * 3600L), expedition.activityOf(second.id()).predecessors())
        );

        execution.startActivity(first.id(), DAY, expedition.activityOf(first.id()).predecessors());
        assertThrows(
                InvalidActivityExecution.class,
                () -> execution.startActivity(second.id(), DAY.plusSeconds(4 * 3600L), expedition.activityOf(second.id()).predecessors())
        );
    }

    @Test
    void startsDependentAfterPredecessorFinishes() {
        Expedition expedition = approvedWithTwoDependentActivities();
        Activity first = expedition.activities().getFirst();
        Activity second = expedition.activities().getLast();
        ExpeditionExecution execution = ExpeditionExecution.started(expedition.id());
        execution.startActivity(first.id(), DAY, expedition.activityOf(first.id()).predecessors());
        execution.finishActivity(first.id(), DAY.plusSeconds(4 * 3600L), "site reached");

        execution.startActivity(second.id(), DAY.plusSeconds(4 * 3600L), expedition.activityOf(second.id()).predecessors());

        assertEquals(2, execution.activities().size());
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
        Activity activity = expedition.activities().getFirst();
        ExpeditionExecution execution = ExpeditionExecution.started(expedition.id());
        execution.startActivity(activity.id(), DAY, expedition.activityOf(activity.id()).predecessors());
        execution.finishActivity(activity.id(), DAY.plusSeconds(3600), "samples stored");

        execution.finish(Set.of(activity.id()));

        assertAll(
                () -> assertEquals(ExpeditionStatus.APPROVED, expedition.status()),
                () -> assertEquals(ExpeditionExecution.Status.FINISHED, execution.status())
        );
    }

    @Test
    void cannotFinishWhileActivitiesRemainOpen() {
        Expedition expedition = approvedWithActivity();
        Activity activity = expedition.activities().getFirst();
        ExpeditionExecution execution = ExpeditionExecution.started(expedition.id());

        assertThrows(InvalidActivityExecution.class, () -> execution.finish(Set.of(activity.id())));

        execution.startActivity(activity.id(), DAY, expedition.activityOf(activity.id()).predecessors());
        assertThrows(InvalidActivityExecution.class, () -> execution.finish(Set.of(activity.id())));
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
    void rejectsDuplicatePermit() {
        Expedition expedition = wetlandDraft();
        PermitId permit = new PermitId(UUID.randomUUID());
        expedition.addPermit(permit);

        assertThrows(InvalidValue.class, () -> expedition.addPermit(permit));
    }

    @Test
    void cannotAssignAfterApproval() {
        Expedition expedition = approvedWithActivity();
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
    void cannotApproveFromDraft() {
        Expedition expedition = wetlandDraft();

        assertThrows(
                InvalidExpeditionTransition.class,
                () -> Approvals.approve(expedition, new ResourceCatalog())
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
                () -> Approvals.approve(expedition, new ResourceCatalog())
        );
        assertEquals(ExpeditionStatus.IN_REVIEW, expedition.status());
    }

    @Test
    void rejectsEditsWhileInReview() {
        Prepared sampling = preparedSampling();
        sampling.expedition().submitForReview();

        assertThrows(
                InvalidExpeditionTransition.class,
                () -> sampling.expedition().addAssignment(new PersonAssignment(
                        sampling.activity().id(),
                        new PersonId(UUID.randomUUID())
                ))
        );
        assertEquals(ExpeditionStatus.IN_REVIEW, sampling.expedition().status());
    }

    @Test
    void cannotApproveWarningWithoutJustification() {
        Prepared crowded = crowdedTransit();
        crowded.expedition().submitForReview();

        assertThrows(
                ExpeditionNotApprovable.class,
                () -> Approvals.approve(crowded.expedition(), crowded.catalog())
        );
    }

    @Test
    void approvesWhenWarningIsJustified() {
        Prepared crowded = crowdedTransit();
        crowded.expedition().submitForReview();
        ValidationResult result = RuleBasedValidator.withDefaultRules()
                .validate(new PlanningContext(crowded.expedition(), crowded.catalog().catalogs(), OccupyingExpeditions.none()));
        result.warnings().forEach(warning -> crowded.expedition().acceptWarning(
                new AcceptedWarning(warning, "backup team on site", crowded.expedition().charter().responsibles().getFirst())
        ));

        Approvals.approve(crowded.expedition(), crowded.catalog());

        assertEquals(ExpeditionStatus.APPROVED, crowded.expedition().status());
    }

    @Test
    void suspendRequiresAnInProgressRun() {
        ExpeditionExecution execution = ExpeditionExecution.started(new ExpeditionId(UUID.randomUUID()));

        execution.suspend();

        assertEquals(ExpeditionExecution.Status.SUSPENDED, execution.status());
        assertThrows(InvalidExpeditionTransition.class, execution::suspend);
    }

    @Test
    void resumeReturnsToInProgress() {
        Expedition expedition = approvedWithActivity();
        ExpeditionExecution execution = ExpeditionExecution.started(expedition.id());
        execution.suspend();

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
                () -> assertEquals(1, expedition.activities().size()),
                () -> assertEquals("Camp to site", expedition.activities().getFirst().name())
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
    void aParallelBlockInsideASequenceKeepsLeavesAndTreeDuration() {
        Expedition expedition = wetlandDraft();
        Activity approach = transit("Approach", 0, 2, DELTA);
        Activity left = sampling();
        Activity right = measurement();
        Activity back = transit("Return", 4, 6, DELTA);
        expedition.addBlock(ActivityBlock.sequential(approach, ActivityBlock.parallel(left, right), back));

        assertAll(
                () -> assertEquals(List.of(approach.id(), left.id(), right.id(), back.id()), activityIds(expedition)),
                () -> assertEquals(Duration.ofHours(8), expedition.estimatedDuration()),
                () -> assertEquals(RiskLevel.HIGH, expedition.estimatedRisk())
        );
    }

    @Test
    void cannotReturnAnApprovedPlanToDraft() {
        Expedition expedition = approvedWithActivity();

        assertThrows(InvalidExpeditionTransition.class, expedition::returnToDraft);

        assertEquals(ExpeditionStatus.APPROVED, expedition.status());
    }

    @Test
    void cannotReturnToDraftWhileTheRunIsInProgress() {
        Expedition expedition = approvedWithActivity();
        Activity activity = expedition.activities().getFirst();
        ExpeditionExecution execution = ExpeditionExecution.started(expedition.id());
        execution.startActivity(activity.id(), DAY, expedition.activityOf(activity.id()).predecessors());

        assertThrows(InvalidExpeditionTransition.class, expedition::returnToDraft);

        assertAll(
                () -> assertEquals(ExpeditionStatus.APPROVED, expedition.status()),
                () -> assertEquals(1, execution.activities().size())
        );
    }

    @Test
    void cannotReturnToDraftFromFinished() {
        Expedition expedition = approvedWithActivity();
        Activity activity = expedition.activities().getFirst();
        ExpeditionExecution execution = ExpeditionExecution.started(expedition.id());
        execution.startActivity(activity.id(), DAY, expedition.activityOf(activity.id()).predecessors());
        execution.finishActivity(activity.id(), DAY.plusSeconds(3600), "samples stored");
        execution.finish(Set.of(activity.id()));

        assertThrows(InvalidExpeditionTransition.class, () -> expedition.returnToDraft());
        assertEquals(ExpeditionExecution.Status.FINISHED, execution.status());
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
                new ExpeditionCharter(
                        List.of(new Objective("Map wetland biodiversity")),
                        window(0, 10),
                        List.of(DELTA),
                        List.of(new PersonId(UUID.randomUUID())),
                        List.of(new Restriction("No night work"))
                )
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
        ExpeditionExecution execution = ExpeditionExecution.started(expedition.id());
        Observation observation = new Observation("site wet", DAY);

        execution.addObservation(observation);

        assertEquals(List.of(observation), execution.observations());
    }

    @Test
    void recordsIncidentWhileInProgress() {
        Expedition expedition = approvedWithActivity();
        ExpeditionExecution execution = ExpeditionExecution.started(expedition.id());
        Incident incident = Incident.of("rain delay", DAY);

        execution.addIncident(incident);

        assertEquals(List.of(incident), execution.incidents());
    }

    @Test
    void tracksActivityExecution() {
        Expedition expedition = approvedWithActivity();
        Activity activity = expedition.activities().getFirst();
        ExpeditionExecution execution = ExpeditionExecution.started(expedition.id());
        execution.startActivity(activity.id(), DAY, expedition.activityOf(activity.id()).predecessors());

        execution.finishActivity(activity.id(), DAY.plusSeconds(3600), "samples stored");

        assertAll(
                () -> assertEquals(1, execution.activities().size()),
                () -> assertTrue(execution.activities().getFirst().isFinished())
        );
    }

    @Test
    void cannotFinishActivityTwice() {
        Expedition expedition = approvedWithActivity();
        Activity activity = expedition.activities().getFirst();
        ExpeditionExecution execution = ExpeditionExecution.started(expedition.id());
        execution.startActivity(activity.id(), DAY, expedition.activityOf(activity.id()).predecessors());
        execution.finishActivity(activity.id(), DAY.plusSeconds(3600), "samples stored");

        assertThrows(
                InvalidActivityExecution.class,
                () -> execution.finishActivity(activity.id(), DAY.plusSeconds(7200), "samples stored again")
        );
    }

    @Test
    void finishingAReturnedExecutionDoesNotChangeTheExpedition() {
        Expedition expedition = approvedWithActivity();
        Activity activity = expedition.activities().getFirst();
        ExpeditionExecution execution = ExpeditionExecution.started(expedition.id());
        execution.startActivity(activity.id(), DAY, expedition.activityOf(activity.id()).predecessors());

        execution.activities().getFirst().finish(DAY.plusSeconds(3600), "samples stored");

        assertFalse(execution.activities().getFirst().isFinished());
    }

    private Expedition approvedWithActivity() {
        Prepared prepared = preparedSampling();
        prepared.expedition().submitForReview();
        Approvals.approve(prepared.expedition(), prepared.catalog());
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
        catalog.save(person(personId, certificationOf(first)));
        catalog.save(new Vehicle(vehicleId, new Passengers(2), Availability.always()));
        catalog.save(firstPermit);
        catalog.save(secondPermit);
        expedition.submitForReview();
        Approvals.approve(expedition, catalog);
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
        catalog.save(person(personId, certificationOf(activity)));
        catalog.save(permit);
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
        catalog.save(new Person(ada, "Ada", List.of(), Availability.always()));
        catalog.save(new Person(bob, "Bob", List.of(), Availability.always()));
        catalog.save(new Vehicle(vehicleId, new Passengers(1), Availability.always()));
        catalog.save(permit);
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
        return Permit.zone(new PermitId(UUID.randomUUID()), activity.zone(), activity.window());
    }

    private static Expedition wetlandDraft() {
        return Expedition.draft(
                new ExpeditionId(UUID.randomUUID()),
                new ExpeditionCharter(
                        List.of(new Objective("Map wetland biodiversity")),
                        new TimePeriod(DAY, DAY.plusSeconds(86_400 * 5)),
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
        return Activity.measurement(new CertificationId(UUID.randomUUID()), new edu.itba.fieldops.domain.shared.InstrumentKind("probe"))
                .named(new ActivityId(UUID.randomUUID()), "Water measurement")
                .estimated(Duration.ofHours(3), RiskLevel.HIGH)
                .in(DELTA, window(6, 9))
                .build();
    }

    private static TimePeriod window(int fromHour, int toHour) {
        return new TimePeriod(DAY.plusSeconds(fromHour * 3600L), DAY.plusSeconds(toHour * 3600L));
    }

    private static List<ActivityId> activityIds(Expedition expedition) {
        return expedition.activities().stream().map(Activity::id).toList();
    }

    private record Prepared(Expedition expedition, ResourceCatalog catalog, Activity activity) {
    }
}
