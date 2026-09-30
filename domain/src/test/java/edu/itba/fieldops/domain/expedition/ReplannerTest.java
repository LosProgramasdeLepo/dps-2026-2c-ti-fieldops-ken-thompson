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
import java.util.Optional;
import java.util.Set;
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
        catalog.add(ada);

        replanner.cancel(expedition, first.id(), catalog.bookable(), OccupyingExpeditions.none());

        assertEquals(List.of(second.id()), expedition.itinerary().stream().map(Activity::id).toList());
        assertTrue(expedition.activityOf(second.id()).predecessors().isEmpty());
        assertEquals(List.of(new PersonAssignment(second.id(), ada.id())), expedition.assignments().all());
    }

    @Test
    void delayFromReviewReturnsToDraftThenShifts() {
        Certification certification = certification();
        Person ada = person(certification);
        Activity sample = sampling(certification.id(), 0, 4);
        Expedition expedition = draftWith(sample);
        expedition.addAssignment(new PersonAssignment(sample.id(), ada.id()));
        expedition.submitForReview();
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(ada);

        replanner.delay(expedition, sample.id(), Duration.ofHours(2), catalog.bookable(), OccupyingExpeditions.none());

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

        Expedition revision = approved.reviseAsDraft();
        replanner.cancel(revision, ride.id(), catalog.bookable(), OccupyingExpeditions.none());

        assertNotEquals(approved.id(), revision.id());
        assertEquals(ExpeditionStatus.APPROVED, approved.status());
        assertEquals(List.of(sample.id(), ride.id()), approved.itinerary().stream().map(Activity::id).toList());
        assertEquals(ExpeditionStatus.DRAFT, revision.status());
        assertEquals(2, revision.version());
        assertEquals(Optional.of(approved.id()), revision.supersedes());
        assertEquals(List.of(sample.id()), revision.itinerary().stream().map(Activity::id).toList());
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
        execution.addIncident(new Incident("ventisca en el frente", DAY, sample.id()));

        Expedition revision = approved.reviseAsDraft();
        replanner.cancel(revision, ride.id(), catalog.bookable(), OccupyingExpeditions.none());

        assertEquals(1, execution.executions().size());
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

        Expedition revision = approved.reviseAsDraft();
        replanner.cancel(
                revision,
                ride.id(),
                catalog.bookable(),
                OccupyingExpeditions.of(revision, List.of(approved))
        );

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
        catalog.add(ada);
        catalog.add(permit);
        Approvals.approve(expedition, catalog);
        ExpeditionExecution execution = ExpeditionExecution.started(expedition.id());
        execution.startActivity(sample.id(), DAY, expedition.activityOf(sample.id()).predecessors());

        Expedition revision = expedition.reviseAsDraft();
        replanner.delay(revision, sample.id(), Duration.ofHours(2), catalog.bookable(), OccupyingExpeditions.none());

        assertEquals(ExpeditionStatus.APPROVED, expedition.status());
        assertEquals(window(0, 4), expedition.activityOf(sample.id()).window());
        assertEquals(1, execution.executions().size());
        assertEquals(ExpeditionStatus.DRAFT, revision.status());
        assertEquals(window(2, 6), revision.activityOf(sample.id()).window());
        assertNotEquals(expedition.id(), revision.id());
    }

    @Test
    void replaceUnavailableReturnsReviewToDraft() {
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
        expedition.submitForReview();
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(ada);
        catalog.add(bob);

        replanner.replaceUnavailable(expedition, catalog.bookable(), OccupyingExpeditions.of(expedition, List.of(occupying)));

        assertEquals(ExpeditionStatus.DRAFT, expedition.status());
        assertEquals(List.of(new PersonAssignment(sample.id(), bob.id())), expedition.assignments().all());
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
        catalog.add(ada);

        replanner.cancel(expedition, ride.id(), catalog.bookable(), OccupyingExpeditions.none());

        assertEquals(List.of(sample.id()), expedition.itinerary().stream().map(Activity::id).toList());
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
        catalog.add(ada);
        catalog.add(bob);

        replanner.replaceUnavailable(expedition, catalog.bookable(), OccupyingExpeditions.of(expedition, List.of(occupying)));

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
        catalog.add(ada);
        catalog.add(bob);
        catalog.add(vials);

        replanner.replaceUnavailable(expedition, catalog.bookable(), OccupyingExpeditions.of(expedition, List.of(occupying)));

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
        catalog.add(ada);

        replanner.delay(expedition, first.id(), Duration.ofHours(2), catalog.bookable(), OccupyingExpeditions.none());

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
        catalog.add(ada);
        catalog.add(bob);

        replanner.delay(expedition, sample.id(), Duration.ofHours(4), catalog.bookable(), OccupyingExpeditions.of(expedition, List.of(occupying)));

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
                () -> replanner.delay(expedition, sample.id(), Duration.ofDays(10), catalog.bookable(), OccupyingExpeditions.none())
        );
    }

    private static ResourceCatalog catalogWith(Person ada, Vehicle vehicle, Permit samplePermit, Permit ridePermit) {
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(ada);
        catalog.add(vehicle);
        catalog.add(samplePermit);
        catalog.add(ridePermit);
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
        return new Permit(new PermitId(UUID.randomUUID()), activity.zone(), activity.window());
    }

    private static Expedition draftWith(Activity activity) {
        Expedition expedition = Expedition.draft(
                new ExpeditionId(UUID.randomUUID()),
                List.of(new Objective("Map wetland biodiversity")),
                week(),
                List.of(DELTA),
                List.of(new PersonId(UUID.randomUUID())),
                List.of(new Restriction("No night work"))
        );
        expedition.addActivity(activity);
        return expedition;
    }

    private static Activity sampling(CertificationId certificationId, int fromHour, int toHour) {
        return Activity.sampling(
                new ActivityId(UUID.randomUUID()),
                "sample",
                Duration.ofHours(toHour - fromHour),
                RiskLevel.MEDIUM,
                window(fromHour, toHour),
                Set.of(),
                DELTA,
                certificationId
        );
    }

    private static Activity transit(int fromHour, int toHour) {
        return Activity.transit(
                new ActivityId(UUID.randomUUID()),
                "transit",
                Duration.ofHours(toHour - fromHour),
                RiskLevel.LOW,
                window(fromHour, toHour),
                Set.of(),
                DELTA
        );
    }

    private static TimePeriod window(int fromHour, int toHour) {
        return new TimePeriod(DAY.plusSeconds(fromHour * 3600L), DAY.plusSeconds(toHour * 3600L));
    }

    private static TimePeriod week() {
        return new TimePeriod(DAY, DAY.plusSeconds(86_400L * 5));
    }
}
