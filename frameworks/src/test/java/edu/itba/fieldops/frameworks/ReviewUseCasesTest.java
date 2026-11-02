package edu.itba.fieldops.frameworks;

import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.expedition.AcceptedWarning;
import edu.itba.fieldops.domain.expedition.ExpeditionNotApprovable;
import edu.itba.fieldops.domain.expedition.ExpeditionStatus;
import edu.itba.fieldops.domain.expedition.InvalidExpeditionTransition;
import edu.itba.fieldops.domain.expedition.Justification;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewUseCasesTest extends UseCaseFixture {

    @Test
    void cannotSubmitWhileCriticalIssuesRemain() {
        Sampling open = unassignedSampling(Map.of());

        assertThrows(ExpeditionNotApprovable.class, () -> review.submit(open.expeditionId()));

        assertEquals(ExpeditionStatus.DRAFT, consult.of(open.expeditionId()).status());
    }

    @Test
    void returnsToDraftFromReviewWithoutStartingARun() {
        Sampling sampling = samplingPlan();
        review.submit(sampling.expeditionId());

        review.returnToDraft(sampling.expeditionId());

        assertEquals(ExpeditionStatus.DRAFT, consult.of(sampling.expeditionId()).status());
        assertTrue(runs.find(sampling.expeditionId()).isEmpty());
    }

    @Test
    void approvesOnceTheResponsibleJustifiesTheWarning() {
        ExpeditionId expeditionId = crowdedCrossingInReview();
        PersonId responsible = consult.of(expeditionId).charter().responsibles().getFirst();
        ValidationIssue capacity = review.validate(expeditionId).warnings().getFirst();
        review.acceptWarning(expeditionId, new AcceptedWarning(capacity, new Justification("second trip planned"), responsible));

        approval.approve(expeditionId);

        assertEquals(ExpeditionStatus.APPROVED, consult.of(expeditionId).status());
    }

    @Test
    void cannotApproveOnceACriticalIssueAppearsDuringReview() {
        Sampling sampling = samplingPlan();
        review.submit(sampling.expeditionId());
        personnel.changeAvailability(sampling.responsible(), new Availability(List.of()));

        assertThrows(ExpeditionNotApprovable.class, () -> approval.approve(sampling.expeditionId()));

        assertEquals(ExpeditionStatus.IN_REVIEW, consult.of(sampling.expeditionId()).status());
    }

    @Test
    void theOriginalStaysApprovedUntilItsRevisionIsApproved() {
        TwoSamplings plan = approvedTwoSamplings();

        ExpeditionId revision = revisionWithout(plan.expeditionId(), plan.later());

        assertEquals(ExpeditionStatus.APPROVED, consult.of(plan.expeditionId()).status());

        review.submit(revision);
        approval.approve(revision);

        assertEquals(ExpeditionStatus.SUPERSEDED, consult.of(plan.expeditionId()).status());
        assertEquals(ExpeditionStatus.APPROVED, consult.of(revision).status());
    }

    @Test
    void onlyOneRevisionOfAPlanCanBeApproved() {
        TwoSamplings plan = approvedTwoSamplings();
        ExpeditionId withoutLater = revisionWithout(plan.expeditionId(), plan.later());
        ExpeditionId withoutFirst = revisionWithout(plan.expeditionId(), plan.first());
        review.submit(withoutLater);
        approval.approve(withoutLater);
        review.submit(withoutFirst);

        assertThrows(InvalidExpeditionTransition.class, () -> approval.approve(withoutFirst));

        assertEquals(ExpeditionStatus.IN_REVIEW, consult.of(withoutFirst).status());
    }
}
