package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.details.ResourceCatalog;
import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Certification;
import edu.itba.fieldops.domain.catalog.Consumable;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.InvalidItinerary;
import edu.itba.fieldops.domain.shared.InvalidExpeditionTransition;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.Stock;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplannerTest {
    private static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");
    private static final WorkZone DELTA = new WorkZone("Delta");

    private final Replanner replanner = new Replanner(new AssignmentSuggester());
    private final Certification certification = new Certification(new CertificationId(UUID.randomUUID()), "Sampling");
    private final Person ada = person("Ada");
    private final Person bob = person("Bob");
    private final ResourceCatalog catalog = new ResourceCatalog();

    @Test
    void cancelRemovesPredecessorKeepsDependentAndFillsItsGaps() {
        catalog.save(ada);
        Activity first = sampling(0, 4);
        Activity second = sampling(4, 8);
        Expedition expedition = draftWith(first);
        expedition.addActivity(second);
        expedition.addDependency(second.id(), first.id());

        replanner.cancel(unoccupied(expedition), first.id());

        assertEquals(List.of(second.id()), expedition.activities().stream().map(Activity::id).toList());
        assertTrue(expedition.activityOf(second.id()).predecessors().isEmpty());
        assertEquals(List.of(new PersonAssignment(second.id(), ada.id())), expedition.assignments().all());
    }

    @Test
    void delayShiftsDependentActivityAndKeepsValidAssignment() {
        catalog.save(ada);
        Activity first = sampling(0, 4);
        Activity second = sampling(4, 8);
        Expedition expedition = draftWith(first);
        expedition.addActivity(second);
        expedition.addDependency(second.id(), first.id());
        expedition.addAssignment(new PersonAssignment(first.id(), ada.id()));

        replanner.delay(unoccupied(expedition), first.id(), Duration.ofHours(2));

        assertEquals(window(2, 6), expedition.activityOf(first.id()).window());
        assertEquals(window(6, 10), expedition.activityOf(second.id()).window());
        assertEquals(
                List.of(new PersonAssignment(first.id(), ada.id()), new PersonAssignment(second.id(), ada.id())),
                expedition.assignments().all()
        );
    }

    @Test
    void delayReplacesPersonWhoWouldOverlapAnOccupyingExpedition() {
        catalog.save(ada);
        catalog.save(bob);
        Expedition occupying = inReviewWithAdaBetween(4, 8);
        Activity sample = sampling(0, 4);
        Expedition expedition = draftWith(sample);
        expedition.addAssignment(new PersonAssignment(sample.id(), ada.id()));

        replanner.delay(occupiedBy(expedition, occupying), sample.id(), Duration.ofHours(4));

        assertEquals(window(4, 8), expedition.activityOf(sample.id()).window());
        assertEquals(List.of(new PersonAssignment(sample.id(), bob.id())), expedition.assignments().all());
    }

    @Test
    void delayOutsideExpeditionPeriodIsRejected() {
        Activity sample = sampling(0, 4);
        Expedition expedition = draftWith(sample);

        assertThrows(InvalidItinerary.class, () -> replanner.delay(unoccupied(expedition), sample.id(), Duration.ofDays(10)));
    }

    @Test
    void replaceUnavailableSwapsAPersonTakenByAnotherPlan() {
        catalog.save(ada);
        catalog.save(bob);
        Expedition occupying = inReviewWithAdaBetween(0, 4);
        Activity sample = sampling(0, 4);
        Expedition expedition = draftWith(sample);
        expedition.addAssignment(new PersonAssignment(sample.id(), ada.id()));

        replanner.replaceUnavailable(occupiedBy(expedition, occupying));

        assertEquals(List.of(new PersonAssignment(sample.id(), bob.id())), expedition.assignments().all());
    }

    @Test
    void replaceUnavailableKeepsConsumable() {
        Consumable vials = new Consumable(new ConsumableId(UUID.randomUUID()), "vials", new Stock(10));
        catalog.save(ada);
        catalog.save(bob);
        catalog.save(vials);
        Expedition occupying = inReviewWithAdaBetween(0, 4);
        Activity sample = sampling(0, 4);
        ConsumableAssignment vialsAssigned = new ConsumableAssignment(sample.id(), vials.id(), new Stock(3));
        Expedition expedition = draftWith(sample);
        expedition.addAssignment(new PersonAssignment(sample.id(), ada.id()));
        expedition.addAssignment(vialsAssigned);

        replanner.replaceUnavailable(occupiedBy(expedition, occupying));

        assertEquals(2, expedition.assignments().all().size());
        assertTrue(expedition.assignments().all().contains(vialsAssigned));
        assertTrue(expedition.assignments().all().contains(new PersonAssignment(sample.id(), bob.id())));
    }

    @Test
    void aRevisionDoesNotCompeteForResourcesWithThePlanItReplaces() {
        catalog.save(ada);
        catalog.save(bob);
        Activity sample = sampling(0, 4);
        Expedition approved = draftWith(sample);
        approved.addAssignment(new PersonAssignment(sample.id(), ada.id()));
        approved.submitForReview();
        approved.markApproved();
        Expedition revision = approved.reviseAsDraft(new ExpeditionId(UUID.randomUUID()));

        replanner.replaceUnavailable(occupiedBy(revision, approved));

        assertEquals(List.of(new PersonAssignment(sample.id(), ada.id())), revision.assignments().all());
    }

    @Test
    void replanRequiresADraftAndLeavesAReviewUntouched() {
        catalog.save(ada);
        Activity sample = sampling(0, 4);
        Expedition expedition = draftWith(sample);
        expedition.addAssignment(new PersonAssignment(sample.id(), ada.id()));
        expedition.submitForReview();
        PlanningContext context = unoccupied(expedition);

        assertThrows(InvalidExpeditionTransition.class, () -> replanner.replaceUnavailable(context));

        assertEquals(ExpeditionStatus.IN_REVIEW, expedition.status());
    }

    private PlanningContext unoccupied(Expedition plan) {
        return new PlanningContext(plan, catalog.catalogs(), OccupyingExpeditions.of(plan, List.of(), Map.of()));
    }

    private PlanningContext occupiedBy(Expedition plan, Expedition other) {
        return new PlanningContext(plan, catalog.catalogs(), OccupyingExpeditions.of(plan, List.of(other), Map.of()));
    }

    private Expedition inReviewWithAdaBetween(int fromHour, int toHour) {
        Activity activity = sampling(fromHour, toHour);
        Expedition expedition = draftWith(activity);
        expedition.addAssignment(new PersonAssignment(activity.id(), ada.id()));
        expedition.submitForReview();
        return expedition;
    }

    private Person person(String name) {
        return new Person(new PersonId(UUID.randomUUID()), name, List.of(certification), Availability.always());
    }

    private Activity sampling(int fromHour, int toHour) {
        return Activity.sampling(certification.id())
                .named(new ActivityId(UUID.randomUUID()), "sample")
                .estimated(Duration.ofHours(toHour - fromHour), RiskLevel.MEDIUM)
                .in(DELTA, window(fromHour, toHour))
                .build();
    }

    private static Expedition draftWith(Activity activity) {
        Expedition expedition = Expedition.draft(
                new ExpeditionId(UUID.randomUUID()),
                new ExpeditionCharter(
                        List.of(new Objective("Map wetland biodiversity")),
                        new TimePeriod(DAY, DAY.plus(Duration.ofDays(5))),
                        List.of(DELTA),
                        List.of(new PersonId(UUID.randomUUID())),
                        List.of(new Restriction("No night work"))
                )
        );
        expedition.addActivity(activity);
        return expedition;
    }

    private static TimePeriod window(int fromHour, int toHour) {
        return new TimePeriod(DAY.plus(Duration.ofHours(fromHour)), DAY.plus(Duration.ofHours(toHour)));
    }
}
