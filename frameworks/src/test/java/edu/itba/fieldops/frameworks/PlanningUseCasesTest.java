package edu.itba.fieldops.frameworks;

import edu.itba.fieldops.domain.expedition.Assignment;
import edu.itba.fieldops.domain.expedition.ExpeditionStatus;
import edu.itba.fieldops.domain.expedition.PersonAssignment;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.ActivityBlock;
import edu.itba.fieldops.domain.itinerary.ActivityName;
import edu.itba.fieldops.domain.itinerary.EstimatedDuration;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.domain.usecase.expedition.PlanSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanningUseCasesTest extends UseCaseFixture {
    @Test
    void suggestsAssignmentsWithoutApplyingThem() {
        Sampling open = unassignedSampling();

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
    void addingTheSequencePredecessorStoresItExplicitly() {
        CertificationId certificationId = certification();
        ExpeditionId expeditionId = draftResponsibleFor(certifiedPerson("Ada", certificationId));
        Activity approach = Activity.transit()
                .named(itinerary.nextActivityId(), new ActivityName("Approach"))
                .estimated(new EstimatedDuration(Duration.ofHours(2)), RiskLevel.LOW)
                .in(DELTA, hours(0, 2))
                .build();
        Activity survey = sampling(certificationId, itinerary.nextActivityId(), hours(2, 4));
        itinerary.addBlock(expeditionId, ActivityBlock.sequential(approach, survey));

        itinerary.addDependency(expeditionId, survey.id(), approach.id());

        assertEquals(Set.of(approach.id()), consult.activity(expeditionId, survey.id()).predecessors());
    }

    @Test
    void attachingAPermitAgainLeavesItOnce() {
        TwoSamplings plan = twoSamplings();
        PermitId permitId = consult.of(plan.expeditionId()).permits().getFirst();

        assignments.addPermit(plan.expeditionId(), permitId);

        assertEquals(List.of(permitId), consult.of(plan.expeditionId()).permits());
    }

    @Test
    void addingADependencyAgainLeavesItOnce() {
        TwoSamplings plan = twoSamplings();
        itinerary.addDependency(plan.expeditionId(), plan.later(), plan.first());

        itinerary.addDependency(plan.expeditionId(), plan.later(), plan.first());

        assertEquals(Set.of(plan.first()), consult.activity(plan.expeditionId(), plan.later()).predecessors());
    }

    @Test
    void consultsAnActivityOfThePlan() {
        Sampling sampling = samplingPlan();

        Activity activity = consult.activity(sampling.expeditionId(), sampling.activityId());

        assertEquals("Soil sampling", activity.name().value());
    }

    @Test
    void rejectsConsultingAnActivityOutsideThePlan() {
        Sampling sampling = samplingPlan();

        assertThrows(UnknownResource.class, () -> consult.activity(sampling.expeditionId(), new ActivityId(UUID.randomUUID())));
    }
}
