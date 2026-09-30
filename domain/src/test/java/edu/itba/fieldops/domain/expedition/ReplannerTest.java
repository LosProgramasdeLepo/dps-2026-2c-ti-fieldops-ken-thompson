package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Certification;
import edu.itba.fieldops.domain.catalog.Consumable;
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
import edu.itba.fieldops.domain.itinerary.InvalidItinerary;
import edu.itba.fieldops.domain.shared.InvalidExpeditionTransition;
import edu.itba.fieldops.domain.shared.Passengers;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.Stock;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.tracking.ExpeditionExecution;
import edu.itba.fieldops.domain.tracking.Incident;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplannerTest {
    private final Replanner replanner = new Replanner(new AssignmentSuggester());

    private static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");
    private static final WorkZone DELTA = new WorkZone("Delta");

    @Test
    void cancelRemovesPredecessorAndKeepsDependent() {
        Certification certification = certification();
        Person ada = person(certification);
        Activity first = sampling(certification.id(), 0, 4);
        Activity second = sampling(certification.id(), 4, 8);
        Expedition expedition = draftWith(first);
        expedition.addActivity(second);
        expedition.addDependency(second.id(), first.id());
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(ada);

        replanner.cancel(contextOf(expedition, catalog), first.id());

        assertEquals(List.of(second.id()), expedition.activities().stream().map(Activity::id).toList());
        assertTrue(expedition.activityOf(second.id()).predecessors().isEmpty());
        assertEquals(List.of(new PersonAssignment(second.id(), ada.id())), expedition.assignments().all());
    }

    @Test
    void delayShiftsTheWindowAndKeepsItsAssignments() {
        Certification certification = certification();
        Person ada = person(certification);
        Activity sample = sampling(certification.id(), 0, 4);
        Expedition expedition = draftWith(sample);
        expedition.addAssignment(new PersonAssignment(sample.id(), ada.id()));
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(ada);

        replanner.delay(contextOf(expedition, catalog), sample.id(), Duration.ofHours(2));

        assertEquals(ExpeditionStatus.DRAFT, expedition.status());
        assertEquals(window(2, 6), expedition.activityOf(sample.id()).window());
        assertEquals(List.of(new PersonAssignment(sample.id(), ada.id())), expedition.assignments().all());
    }

    @Test
    void cancelFromApprovedLeavesTheApprovedPlanAndReturnsARevision() {
        Certification certification = certification();
        Person ada = person(certification);
        Activity sample = sampling(certification.id(), 0, 4);
        Activity ride = transit(4, 6);
        Vehicle vehicle = vehicle();
        Expedition approved = draftWith(sample);
        approved.addActivity(ride);
        approved.addAssignment(new PersonAssignment(sample.id(), ada.id()));
        approved.addAssignment(new VehicleAssignment(ride.id(), vehicle.id()));
        Permit samplePermit = permitFor(sample);
        Permit ridePermit = permitFor(ride);
        approved.addPermit(samplePermit.id());
        approved.addPermit(ridePermit.id());
        approved.submitForReview();
        ResourceCatalog catalog = catalogWith(ada, vehicle, samplePermit, ridePermit);
        Approvals.approve(approved, catalog);

        Expedition revision = approved.reviseAsDraft(new ExpeditionId(UUID.randomUUID()));
        replanner.cancel(contextOf(revision, catalog), ride.id());

        assertNotEquals(approved.id(), revision.id());
        assertEquals(ExpeditionStatus.APPROVED, approved.status());
        assertEquals(List.of(sample.id(), ride.id()), approved.activities().stream().map(Activity::id).toList());
        assertEquals(ExpeditionStatus.DRAFT, revision.status());
        assertEquals(2, revision.version());
        assertEquals(Optional.of(approved.id()), revision.supersedes());
        assertEquals(List.of(sample.id()), revision.activities().stream().map(Activity::id).toList());
        assertEquals(List.of(new PersonAssignment(sample.id(), ada.id())), revision.assignments().all());
    }

    @Test
    void revisingAnApprovedPlanLeavesTheExecutionOnTheOriginal() {
        Certification certification = certification();
        Person ada = person(certification);
        Activity sample = sampling(certification.id(), 0, 4);
        Activity ride = transit(4, 6);
        Vehicle vehicle = vehicle();
        Expedition approved = draftWith(sample);
        approved.addActivity(ride);
        approved.addAssignment(new PersonAssignment(sample.id(), ada.id()));
        approved.addAssignment(new VehicleAssignment(ride.id(), vehicle.id()));
        Permit samplePermit = permitFor(sample);
        Permit ridePermit = permitFor(ride);
        approved.addPermit(samplePermit.id());
        approved.addPermit(ridePermit.id());
        approved.submitForReview();
        ResourceCatalog catalog = catalogWith(ada, vehicle, samplePermit, ridePermit);
        Approvals.approve(approved, catalog);
        ExpeditionExecution execution = ExpeditionExecution.started(approved.id());
        execution.startActivity(sample.id(), DAY, approved.activityOf(sample.id()).predecessors());
        execution.addIncident(Incident.affecting(sample.id(), "ventisca en el frente", DAY));

        Expedition revision = approved.reviseAsDraft(new ExpeditionId(UUID.randomUUID()));
        replanner.cancel(contextOf(revision, catalog), ride.id());

        assertEquals(1, execution.activities().size());
        assertEquals(1, execution.incidents().size());
        assertEquals(ExpeditionExecution.Status.IN_PROGRESS, execution.status());
        assertEquals(approved.id(), execution.expeditionId());
        assertNotEquals(approved.id(), revision.id());
        assertEquals(ExpeditionStatus.APPROVED, approved.status());
        assertEquals(window(0, 4), approved.activityOf(sample.id()).window());
    }

    @Test
    void aRevisionDoesNotCompeteForResourcesWithThePlanItReplaces() {
        Certification certification = certification();
        Person ada = person(certification);
        Activity sample = sampling(certification.id(), 0, 4);
        Activity ride = transit(4, 6);
        Vehicle vehicle = vehicle();
        Expedition approved = draftWith(sample);
        approved.addActivity(ride);
        approved.addAssignment(new PersonAssignment(sample.id(), ada.id()));
        approved.addAssignment(new VehicleAssignment(ride.id(), vehicle.id()));
        Permit samplePermit = permitFor(sample);
        Permit ridePermit = permitFor(ride);
        approved.addPermit(samplePermit.id());
        approved.addPermit(ridePermit.id());
        approved.submitForReview();
        ResourceCatalog catalog = catalogWith(ada, vehicle, samplePermit, ridePermit);
        Approvals.approve(approved, catalog);

        Expedition revision = approved.reviseAsDraft(new ExpeditionId(UUID.randomUUID()));
        replanner.cancel(new PlanningContext(revision, catalog.catalogs(), OccupyingExpeditions.of(revision, List.of(approved), Map.of())), ride.id());

        assertEquals(List.of(new PersonAssignment(sample.id(), ada.id())), revision.assignments().all());
    }

    @Test
    void delayFromInProgressShiftsTheRevisionAndLeavesTheRunUntouched() {
        Certification certification = certification();
        Person ada = person(certification);
        Activity sample = sampling(certification.id(), 0, 4);
        Expedition expedition = draftWith(sample);
        expedition.addAssignment(new PersonAssignment(sample.id(), ada.id()));
        Permit permit = permitFor(sample);
        expedition.addPermit(permit.id());
        expedition.submitForReview();
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(ada);
        catalog.save(permit);
        Approvals.approve(expedition, catalog);
        ExpeditionExecution execution = ExpeditionExecution.started(expedition.id());
        execution.startActivity(sample.id(), DAY, expedition.activityOf(sample.id()).predecessors());

        Expedition revision = expedition.reviseAsDraft(new ExpeditionId(UUID.randomUUID()));
        replanner.delay(contextOf(revision, catalog), sample.id(), Duration.ofHours(2));

        assertEquals(ExpeditionStatus.APPROVED, expedition.status());
        assertEquals(window(0, 4), expedition.activityOf(sample.id()).window());
        assertEquals(1, execution.activities().size());
        assertEquals(ExpeditionStatus.DRAFT, revision.status());
        assertEquals(window(2, 6), revision.activityOf(sample.id()).window());
        assertNotEquals(expedition.id(), revision.id());
    }

    @Test
    void replaceUnavailableSwapsAPersonTakenByAnotherPlan() {
        Certification certification = certification();
        Person ada = person(certification);
        Person bob = person(certification);
        Activity occupied = sampling(certification.id(), 0, 4);
        Expedition occupying = draftWith(occupied);
        occupying.addAssignment(new PersonAssignment(occupied.id(), ada.id()));
        occupying.submitForReview();
        Activity sample = sampling(certification.id(), 0, 4);
        Expedition expedition = draftWith(sample);
        expedition.addAssignment(new PersonAssignment(sample.id(), ada.id()));
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(ada);
        catalog.save(bob);

        replanner.replaceUnavailable(new PlanningContext(expedition, catalog.catalogs(), OccupyingExpeditions.of(expedition, List.of(occupying), Map.of())));

        assertEquals(ExpeditionStatus.DRAFT, expedition.status());
        assertEquals(List.of(new PersonAssignment(sample.id(), bob.id())), expedition.assignments().all());
    }

    @Test
    void replanRequiresADraftAndLeavesAReviewUntouched() {
        Certification certification = certification();
        Person ada = person(certification);
        Activity sample = sampling(certification.id(), 0, 4);
        Expedition expedition = draftWith(sample);
        expedition.addAssignment(new PersonAssignment(sample.id(), ada.id()));
        expedition.submitForReview();
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(ada);
        PlanningContext context = contextOf(expedition, catalog);

        assertThrows(InvalidExpeditionTransition.class, () -> replanner.replaceUnavailable(context));

        assertEquals(ExpeditionStatus.IN_REVIEW, expedition.status());
    }

    @Test
    void cancelRemovesActivityAndFillsRemainingGaps() {
        Certification certification = certification();
        Person ada = person(certification);
        Activity sample = sampling(certification.id(), 0, 4);
        Activity ride = transit(4, 6);
        Expedition expedition = draftWith(sample);
        expedition.addActivity(ride);
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(ada);

        replanner.cancel(contextOf(expedition, catalog), ride.id());

        assertEquals(List.of(sample.id()), expedition.activities().stream().map(Activity::id).toList());
        assertEquals(List.of(new PersonAssignment(sample.id(), ada.id())), expedition.assignments().all());
    }

    @Test
    void replaceUnavailableDropsOccupiedPersonAndSuggestsTheNext() {
        Certification certification = certification();
        Person ada = person(certification);
        Person bob = person(certification);
        Activity occupied = sampling(certification.id(), 0, 4);
        Expedition occupying = draftWith(occupied);
        occupying.addAssignment(new PersonAssignment(occupied.id(), ada.id()));
        occupying.submitForReview();
        Activity sample = sampling(certification.id(), 0, 4);
        Expedition expedition = draftWith(sample);
        expedition.addAssignment(new PersonAssignment(sample.id(), ada.id()));
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(ada);
        catalog.save(bob);

        replanner.replaceUnavailable(new PlanningContext(expedition, catalog.catalogs(), OccupyingExpeditions.of(expedition, List.of(occupying), Map.of())));

        assertEquals(List.of(new PersonAssignment(sample.id(), bob.id())), expedition.assignments().all());
    }

    @Test
    void replaceUnavailableKeepsConsumable() {
        Certification certification = certification();
        Person ada = person(certification);
        Person bob = person(certification);
        Consumable vials = new Consumable(new ConsumableId(UUID.randomUUID()), "vials", new Stock(10));
        Activity occupied = sampling(certification.id(), 0, 4);
        Expedition occupying = draftWith(occupied);
        occupying.addAssignment(new PersonAssignment(occupied.id(), ada.id()));
        occupying.submitForReview();
        Activity sample = sampling(certification.id(), 0, 4);
        ConsumableAssignment vialsAssigned = new ConsumableAssignment(sample.id(), vials.id(), new Stock(3));
        Expedition expedition = draftWith(sample);
        expedition.addAssignment(new PersonAssignment(sample.id(), ada.id()));
        expedition.addAssignment(vialsAssigned);
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(ada);
        catalog.save(bob);
        catalog.save(vials);

        replanner.replaceUnavailable(new PlanningContext(expedition, catalog.catalogs(), OccupyingExpeditions.of(expedition, List.of(occupying), Map.of())));

        assertEquals(2, expedition.assignments().all().size());
        assertTrue(expedition.assignments().all().contains(vialsAssigned));
        assertTrue(expedition.assignments().all().contains(new PersonAssignment(sample.id(), bob.id())));
    }

    @Test
    void delayShiftsDependentActivityAndKeepsValidAssignment() {
        Certification certification = certification();
        Person ada = person(certification);
        Activity first = sampling(certification.id(), 0, 4);
        Activity second = sampling(certification.id(), 4, 8);
        Expedition expedition = draftWith(first);
        expedition.addActivity(second);
        expedition.addDependency(second.id(), first.id());
        expedition.addAssignment(new PersonAssignment(first.id(), ada.id()));
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(ada);

        replanner.delay(contextOf(expedition, catalog), first.id(), Duration.ofHours(2));

        assertEquals(window(2, 6), expedition.activityOf(first.id()).window());
        assertEquals(window(6, 10), expedition.activityOf(second.id()).window());
        assertEquals(
                List.of(
                        new PersonAssignment(first.id(), ada.id()),
                        new PersonAssignment(second.id(), ada.id())
                ),
                expedition.assignments().all()
        );
    }

    @Test
    void delayReplacesPersonWhoWouldOverlapAnOccupyingExpedition() {
        Certification certification = certification();
        Person ada = person(certification);
        Person bob = person(certification);
        Activity occupied = sampling(certification.id(), 4, 8);
        Expedition occupying = draftWith(occupied);
        occupying.addAssignment(new PersonAssignment(occupied.id(), ada.id()));
        occupying.submitForReview();
        Activity sample = sampling(certification.id(), 0, 4);
        Expedition expedition = draftWith(sample);
        expedition.addAssignment(new PersonAssignment(sample.id(), ada.id()));
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(ada);
        catalog.save(bob);

        replanner.delay(new PlanningContext(expedition, catalog.catalogs(), OccupyingExpeditions.of(expedition, List.of(occupying), Map.of())), sample.id(), Duration.ofHours(4));

        assertEquals(window(4, 8), expedition.activityOf(sample.id()).window());
        assertEquals(List.of(new PersonAssignment(sample.id(), bob.id())), expedition.assignments().all());
    }

    @Test
    void delayOutsideExpeditionPeriodIsRejected() {
        Certification certification = certification();
        Activity sample = sampling(certification.id(), 0, 4);
        Expedition expedition = draftWith(sample);
        ResourceCatalog catalog = new ResourceCatalog();

        assertThrows(
                InvalidItinerary.class,
                () -> replanner.delay(contextOf(expedition, catalog), sample.id(), Duration.ofDays(10))
        );
    }

    private static PlanningContext contextOf(Expedition plan, ResourceCatalog catalog) {
        return new PlanningContext(plan, catalog.catalogs(), OccupyingExpeditions.none());
    }

    private static ResourceCatalog catalogWith(Person ada, Vehicle vehicle, Permit samplePermit, Permit ridePermit) {
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.save(ada);
        catalog.save(vehicle);
        catalog.save(samplePermit);
        catalog.save(ridePermit);
        return catalog;
    }

    private static Certification certification() {
        return new Certification(new CertificationId(UUID.randomUUID()), "Sampling");
    }

    private static Person person(Certification certification) {
        return new Person(new PersonId(UUID.randomUUID()), "Ada", List.of(certification), Availability.always());
    }

    private static Vehicle vehicle() {
        return new Vehicle(new VehicleId(UUID.randomUUID()), new Passengers(4), Availability.always());
    }

    private static Permit permitFor(Activity activity) {
        return Permit.zone(new PermitId(UUID.randomUUID()), activity.zone(), activity.window());
    }

    private static Expedition draftWith(Activity activity) {
        Expedition expedition = Expedition.draft(
                new ExpeditionId(UUID.randomUUID()),
                new ExpeditionCharter(
                        List.of(new Objective("Map wetland biodiversity")),
                        week(),
                        List.of(DELTA),
                        List.of(new PersonId(UUID.randomUUID())),
                        List.of(new Restriction("No night work"))
                )
        );
        expedition.addActivity(activity);
        return expedition;
    }

    private static Activity sampling(CertificationId certificationId, int fromHour, int toHour) {
        return Activity.sampling(certificationId)
                .named(new ActivityId(UUID.randomUUID()), "sample")
                .estimated(Duration.ofHours(toHour - fromHour), RiskLevel.MEDIUM)
                .in(DELTA, window(fromHour, toHour))
                .build();
    }

    private static Activity transit(int fromHour, int toHour) {
        return Activity.transit()
                .named(new ActivityId(UUID.randomUUID()), "transit")
                .estimated(Duration.ofHours(toHour - fromHour), RiskLevel.LOW)
                .in(DELTA, window(fromHour, toHour))
                .build();
    }

    private static TimePeriod window(int fromHour, int toHour) {
        return new TimePeriod(DAY.plusSeconds(fromHour * 3600L), DAY.plusSeconds(toHour * 3600L));
    }

    private static TimePeriod week() {
        return new TimePeriod(DAY, DAY.plusSeconds(86_400L * 5));
    }
}
