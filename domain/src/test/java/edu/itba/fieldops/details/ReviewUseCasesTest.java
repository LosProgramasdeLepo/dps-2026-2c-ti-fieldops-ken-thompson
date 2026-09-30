package edu.itba.fieldops.details;

import edu.itba.fieldops.domain.assessment.IssueSeverity;
import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.expedition.AcceptedWarning;
import edu.itba.fieldops.domain.expedition.ExpeditionNotApprovable;
import edu.itba.fieldops.domain.expedition.ExpeditionStatus;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.itinerary.InvalidItinerary;
import edu.itba.fieldops.domain.shared.InvalidExpeditionTransition;
import edu.itba.fieldops.domain.shared.InvalidValue;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewUseCasesTest extends UseCaseFixture {
    @Test
    void rejectsSubmittingAnEmptyItinerary() {
        ExpeditionId expeditionId = draftResponsibleFor(registry.registerPerson("Ada", List.of(), Availability.always()));

        assertThrows(InvalidItinerary.class, () -> review.submit(expeditionId));
    }

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
    void onlyAResponsibleCanAcceptAWarning() {
        ExpeditionId expeditionId = crowdedCrossingInReview();
        ValidationIssue capacity = review.validate(expeditionId).warnings().getFirst();
        AcceptedWarning byAStranger = new AcceptedWarning(capacity, "backup team", new PersonId(UUID.randomUUID()));

        InvalidValue rejected = assertThrows(InvalidValue.class, () -> review.acceptWarning(expeditionId, byAStranger));

        assertEquals("warning must be accepted by a responsible", rejected.getMessage());
    }

    @Test
    void rejectsAWarningTheValidationDoesNotRaise() {
        ExpeditionId expeditionId = crowdedCrossingInReview();
        PersonId responsible = consult.of(expeditionId).charter().responsibles().getFirst();
        ValidationIssue invented = new ValidationIssue(IssueSeverity.WARNING, "CAPACITY", "vehicle near capacity");
        AcceptedWarning accepted = new AcceptedWarning(invented, "backup team", responsible);

        InvalidValue rejected = assertThrows(InvalidValue.class, () -> review.acceptWarning(expeditionId, accepted));

        assertEquals("not a current warning: CAPACITY", rejected.getMessage());
    }

    @Test
    void cannotApproveWhileAWarningIsNotJustified() {
        ExpeditionId expeditionId = crowdedCrossingInReview();

        assertThrows(ExpeditionNotApprovable.class, () -> approval.approve(expeditionId));
    }

    @Test
    void approvesOnceTheResponsibleJustifiesTheWarning() {
        ExpeditionId expeditionId = crowdedCrossingInReview();
        PersonId responsible = consult.of(expeditionId).charter().responsibles().getFirst();
        ValidationIssue capacity = review.validate(expeditionId).warnings().getFirst();
        review.acceptWarning(expeditionId, new AcceptedWarning(capacity, "second trip planned", responsible));

        approval.approve(expeditionId);

        assertEquals(ExpeditionStatus.APPROVED, consult.of(expeditionId).status());
    }

    @Test
    void cannotApproveADraft() {
        Sampling sampling = samplingPlan();

        assertThrows(InvalidExpeditionTransition.class, () -> approval.approve(sampling.expeditionId()));
    }

    @Test
    void cannotApproveOnceACriticalIssueAppearsDuringReview() {
        Sampling sampling = samplingPlan();
        review.submit(sampling.expeditionId());
        registry.changeAvailability(sampling.responsible(), new Availability(List.of()));

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
