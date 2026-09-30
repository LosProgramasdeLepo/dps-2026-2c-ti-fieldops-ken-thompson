package edu.itba.fieldops.details;

import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.expedition.ExpeditionStatus;
import edu.itba.fieldops.domain.expedition.ReplanProposal;
import edu.itba.fieldops.domain.expedition.VehicleAssignment;
import edu.itba.fieldops.domain.expedition.usecase.PlanSnapshot;
import edu.itba.fieldops.domain.expedition.usecase.ProposalSnapshot;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.Passengers;
import edu.itba.fieldops.domain.tracking.Incident;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplanningUseCasesTest extends UseCaseFixture {
    @Test
    void anIncidentOnAnActivityProposesAReplanWithoutChangingThePlan() {
        Sampling sampling = runningSampling();
        clock.set(at(2));

        incidents.record(sampling.expeditionId(), "storm on site", sampling.activityId());

        ProposalSnapshot proposal = onlyProposalOf(sampling.expeditionId());
        assertAll(
                () -> assertEquals(Incident.affecting(sampling.activityId(), "storm on site", at(2)), proposal.incident()),
                () -> assertEquals(ReplanProposal.Decision.PENDING, proposal.decision()),
                () -> assertEquals(hours(2, 6), proposal.suggested().itinerary().getFirst().activities().getFirst().window()),
                () -> assertEquals(ExpeditionStatus.APPROVED, consult.of(sampling.expeditionId()).status())
        );
    }

    @Test
    void anIncidentWithoutAnActivityIsRecordedWithoutAProposal() {
        Sampling sampling = runningSampling();

        incidents.record(sampling.expeditionId(), "storm on site");

        assertTrue(proposalReview.of(sampling.expeditionId()).isEmpty());
        assertEquals(List.of(Incident.of("storm on site", DAY)), runs.find(sampling.expeditionId()).orElseThrow().incidents());
    }

    @Test
    void acceptingAProposalKeepsItsPlanAsADraftRevision() {
        Sampling sampling = runningSampling();
        incidents.record(sampling.expeditionId(), "storm on site", sampling.activityId());
        ProposalSnapshot proposal = onlyProposalOf(sampling.expeditionId());

        proposalReview.accept(proposal.id(), sampling.responsible());

        ProposalSnapshot decided = onlyProposalOf(sampling.expeditionId());
        assertAll(
                () -> assertEquals(ReplanProposal.Decision.ACCEPTED, decided.decision()),
                () -> assertEquals(Optional.of(sampling.responsible()), decided.decidedBy()),
                () -> assertEquals(ExpeditionStatus.DRAFT, consult.of(proposal.suggested().id()).status())
        );
    }

    @Test
    void rejectingAProposalDiscardsItsPlan() {
        Sampling sampling = runningSampling();
        incidents.record(sampling.expeditionId(), "equipment failure", sampling.activityId());
        ProposalSnapshot proposal = onlyProposalOf(sampling.expeditionId());

        proposalReview.reject(proposal.id(), sampling.responsible());

        assertAll(
                () -> assertEquals(ReplanProposal.Decision.REJECTED, onlyProposalOf(sampling.expeditionId()).decision()),
                () -> assertTrue(plans.find(proposal.suggested().id()).isEmpty()),
                () -> assertEquals(ExpeditionStatus.APPROVED, consult.of(sampling.expeditionId()).status())
        );
    }

    @Test
    void onlyAResponsibleCanDecideAProposal() {
        Sampling sampling = runningSampling();
        incidents.record(sampling.expeditionId(), "storm on site", sampling.activityId());
        ProposalSnapshot proposal = onlyProposalOf(sampling.expeditionId());
        PersonId stranger = new PersonId(UUID.randomUUID());

        assertThrows(InvalidValue.class, () -> proposalReview.accept(proposal.id(), stranger));

        assertEquals(ReplanProposal.Decision.PENDING, onlyProposalOf(sampling.expeditionId()).decision());
    }

    @Test
    void delayingAPlanUnderReviewReturnsItToDraft() {
        Sampling sampling = samplingPlan();
        review.submit(sampling.expeditionId());

        ExpeditionId delayed = replan.delay(sampling.expeditionId(), sampling.activityId(), Duration.ofHours(1));

        PlanSnapshot plan = consult.of(delayed);
        assertAll(
                () -> assertEquals(sampling.expeditionId(), delayed),
                () -> assertEquals(ExpeditionStatus.DRAFT, plan.status()),
                () -> assertEquals(hours(1, 5), plan.itinerary().getFirst().activities().getFirst().window())
        );
    }

    @Test
    void replanningAnApprovedPlanCreatesTheNextRevision() {
        Sampling sampling = approvedSampling();

        ExpeditionId revision = replan.replaceUnavailable(sampling.expeditionId());

        assertAll(
                () -> assertEquals(ExpeditionStatus.APPROVED, consult.of(sampling.expeditionId()).status()),
                () -> assertEquals(ExpeditionStatus.DRAFT, consult.of(revision).status()),
                () -> assertEquals(2, consult.of(revision).version())
        );
    }

    @Test
    void anUnavailableVehicleIsReplacedInTheRevision() {
        PersonId ada = registry.registerPerson("Ada", List.of(), Availability.always());
        VehicleId boat = registry.registerVehicle(new Passengers(4), Availability.always());
        VehicleId spare = registry.registerVehicle(new Passengers(4), Availability.always());
        Crossing crossing = crossing(ada, boat);
        review.submit(crossing.expeditionId());
        approval.approve(crossing.expeditionId());
        registry.changeAvailability(boat, new Availability(List.of()));

        ExpeditionId revision = replan.replaceUnavailable(crossing.expeditionId());

        assertEquals(List.of(new VehicleAssignment(crossing.activityId(), spare)), consult.of(revision).assignments());
    }

    private ProposalSnapshot onlyProposalOf(ExpeditionId expeditionId) {
        List<ProposalSnapshot> found = proposalReview.of(expeditionId);
        assertEquals(1, found.size());
        return found.getFirst();
    }
}
