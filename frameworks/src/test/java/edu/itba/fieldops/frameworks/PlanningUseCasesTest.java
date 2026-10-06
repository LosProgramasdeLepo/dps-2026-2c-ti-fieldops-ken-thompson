package edu.itba.fieldops.frameworks;

import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.expedition.Assignment;
import edu.itba.fieldops.domain.expedition.ConsumableAssignment;
import edu.itba.fieldops.domain.expedition.ExpeditionCharter;
import edu.itba.fieldops.domain.expedition.ExpeditionStatus;
import edu.itba.fieldops.domain.expedition.InstrumentAssignment;
import edu.itba.fieldops.domain.expedition.Objective;
import edu.itba.fieldops.domain.expedition.PersonAssignment;
import edu.itba.fieldops.domain.expedition.Restriction;
import edu.itba.fieldops.usecase.expedition.PlanSnapshot;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.ActivityBlock;
import edu.itba.fieldops.domain.report.Estimate;
import edu.itba.fieldops.domain.shared.InstrumentKind;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.Stock;
import edu.itba.fieldops.usecase.shared.Page;
import edu.itba.fieldops.usecase.shared.PageRequest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanningUseCasesTest extends UseCaseFixture {
    @Test
    void rejectsAResponsibleThatIsNotInTheCatalog() {
        ExpeditionCharter charter = new ExpeditionCharter(
                List.of(new Objective("Map wetland")),
                PERIOD,
                List.of(DELTA),
                List.of(new PersonId(UUID.randomUUID())),
                List.of(new Restriction("Daylight only"))
        );

        assertThrows(UnknownResource.class, () -> drafts.draft(charter));
    }

    @Test
    void theEstimateOfANightActivityUsesItsRaisedRisk() {
        CertificationId nightOperation = personnel.registerCertification("Night operation");
        PersonId ada = personnel.registerPerson("Ada", List.of(nightOperation), Availability.always());
        InstrumentId lamp = equipment.registerInstrument(InstrumentKind.LIGHTING, Availability.always());
        ExpeditionId expeditionId = draftResponsibleFor(ada);
        ActivityId activityId = itinerary.nextActivityId();
        itinerary.addActivity(expeditionId, Activity.night(nightOperation)
                .named(activityId, "Night survey")
                .estimated(Duration.ofHours(3), RiskLevel.MEDIUM)
                .in(DELTA, hours(0, 3))
                .build());
        assignments.addAssignment(expeditionId, new PersonAssignment(activityId, ada));
        assignments.addAssignment(expeditionId, new InstrumentAssignment(activityId, lamp));

        Estimate estimate = estimates.of(expeditionId);

        assertEquals(RiskLevel.HIGH, estimate.risk());
        assertEquals(Duration.ofHours(3), estimate.duration());
    }

    @Test
    void theEstimateOfAParallelBlockInsideASequenceTakesTheLongestBranch() {
        CertificationId certificationId = certification();
        ExpeditionId expeditionId = draftResponsibleFor(certifiedPerson("Ada", certificationId));
        Activity approach = Activity.transit()
                .named(itinerary.nextActivityId(), "Approach")
                .estimated(Duration.ofHours(2), RiskLevel.LOW)
                .in(DELTA, hours(0, 2))
                .build();
        Activity left = sampling(certificationId, itinerary.nextActivityId(), hours(2, 6));
        Activity right = sampling(certificationId, itinerary.nextActivityId(), hours(2, 5));
        itinerary.addBlock(expeditionId, ActivityBlock.sequential(approach, ActivityBlock.parallel(left, right)));

        Estimate estimate = estimates.of(expeditionId);

        assertEquals(Duration.ofHours(6), estimate.duration());
    }

    @Test
    void estimatesConsumptionFromRequirementsRatherThanAssignments() {
        ConsumableId fuel = new ConsumableId(UUID.randomUUID());
        Sampling sampling = samplingPlan(Map.of(fuel, new Stock(3)));
        ConsumableId vials = equipment.registerConsumable("vials", new Stock(20));
        assignments.addAssignment(sampling.expeditionId(), new ConsumableAssignment(sampling.activityId(), vials, new Stock(7)));

        Estimate estimate = estimates.of(sampling.expeditionId());

        assertEquals(Map.of(fuel, new Stock(3)), estimate.estimatedConsumption());
    }

    @Test
    void suggestsAssignmentsWithoutApplyingThem() {
        Sampling open = unassignedSampling(Map.of());

        List<Assignment> suggested = assignments.suggest(open.expeditionId());

        assertEquals(List.of(new PersonAssignment(open.activityId(), open.responsible())), suggested);
        assertTrue(consult.of(open.expeditionId()).assignments().isEmpty());
    }

    @Test
    void consultsAPlanAsASnapshot() {
        Sampling sampling = samplingPlan();

        PlanSnapshot snapshot = consult.of(sampling.expeditionId());

        assertAll(
                () -> assertEquals(sampling.expeditionId(), snapshot.id()),
                () -> assertEquals(1, snapshot.version()),
                () -> assertFalse(snapshot.supersedes().isPresent()),
                () -> assertEquals(ExpeditionStatus.DRAFT, snapshot.status()),
                () -> assertEquals(1, snapshot.itinerary().size()),
                () -> assertEquals(1, snapshot.assignments().size()),
                () -> assertEquals(1, snapshot.permits().size())
        );
    }

    @Test
    void pagesThroughPlansAndTheirRevisions() {
        Sampling sampling = approvedSampling();
        ExpeditionId revision = replan.revise(sampling.expeditionId());

        Page<PlanSnapshot> page = consult.all(new PageRequest(0, 1));

        assertAll(
                () -> assertEquals(List.of(sampling.expeditionId()), page.items().stream().map(PlanSnapshot::id).toList()),
                () -> assertEquals(2, page.totalItems()),
                () -> assertEquals(revision, consult.all(new PageRequest(1, 1)).items().getFirst().id())
        );
    }

    @Test
    void consultsAnActivityOfThePlan() {
        Sampling sampling = samplingPlan();

        Activity activity = consult.activity(sampling.expeditionId(), sampling.activityId());

        assertEquals("Soil sampling", activity.name());
    }

    @Test
    void rejectsConsultingAnActivityOutsideThePlan() {
        Sampling sampling = samplingPlan();

        assertThrows(UnknownResource.class, () -> consult.activity(sampling.expeditionId(), new ActivityId(UUID.randomUUID())));
    }
}
