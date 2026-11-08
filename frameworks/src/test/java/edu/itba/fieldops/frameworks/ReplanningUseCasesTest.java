package edu.itba.fieldops.frameworks;

import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Capacity;
import edu.itba.fieldops.domain.catalog.PersonName;
import edu.itba.fieldops.domain.event.IncidentReported;
import edu.itba.fieldops.domain.event.ReplanDecided;
import edu.itba.fieldops.domain.event.ReplanProposed;
import edu.itba.fieldops.domain.expedition.ExpeditionStatus;
import edu.itba.fieldops.domain.expedition.ReplanProposal;
import edu.itba.fieldops.domain.expedition.VehicleAssignment;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.IncidentId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.ProposalId;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.itinerary.Delay;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.domain.tracking.IncidentDescription;
import edu.itba.fieldops.domain.usecase.expedition.PlanSnapshot;
import edu.itba.fieldops.domain.usecase.expedition.ProposalSnapshot;
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

        IncidentId incidentId = incidents.record(sampling.expeditionId(), new IncidentDescription("storm on site"), sampling.activityId());

        ProposalSnapshot proposal = onlyProposalOf(sampling.expeditionId());
        assertAll(
                () -> assertEquals(
                        new Incident(incidentId, new IncidentDescription("storm on site"), at(2), Optional.of(sampling.activityId())),
                        proposal.incident()
                ),
                () -> assertEquals(ReplanProposal.Decision.PENDING, proposal.decision()),
                () -> assertEquals(hours(2, 6), proposal.suggested().itinerary().getFirst().activities().getFirst().window()),
                () -> assertEquals(ExpeditionStatus.APPROVED, consult.of(sampling.expeditionId()).status())
        );
    }

    @Test
    void anIncidentWithoutAnActivityIsRecordedWithoutAProposal() {
        Sampling sampling = runningSampling();

        IncidentId incidentId = incidents.record(sampling.expeditionId(), new IncidentDescription("storm on site"));

        assertTrue(proposalReview.of(sampling.expeditionId(), FIRST_PAGE).items().isEmpty());
        assertEquals(
                List.of(new Incident(incidentId, new IncidentDescription("storm on site"), DAY, Optional.empty())),
                runs.find(sampling.expeditionId()).orElseThrow().incidents()
        );
    }

    @Test
    void anIncidentOnAnActivityAppendsIncidentReportedAndThenReplanProposed() {
        Sampling sampling = runningSampling();
        clock.set(at(2));
        int mark = eventMark();

        IncidentId incidentId = incidents.record(sampling.expeditionId(), new IncidentDescription("storm on site"), sampling.activityId());

        ProposalSnapshot proposal = onlyProposalOf(sampling.expeditionId());
        assertEquals(
                List.of(
                        new IncidentReported(
                                sampling.expeditionId(), at(2), incidentId, Optional.of(sampling.activityId()), new IncidentDescription("storm on site")
                        ),
                        new ReplanProposed(sampling.expeditionId(), at(2), proposal.id(), proposal.suggested().id(), incidentId)
                ),
                eventsSince(mark)
        );
    }

    @Test
    void anIncidentWithoutAnActivityAppendsOnlyIncidentReported() {
        Sampling sampling = runningSampling();
        int mark = eventMark();

        IncidentId incidentId = incidents.record(sampling.expeditionId(), new IncidentDescription("storm on site"));

        assertEquals(
                List.of(new IncidentReported(sampling.expeditionId(), DAY, incidentId, Optional.empty(), new IncidentDescription("storm on site"))),
                eventsSince(mark)
        );
    }

    @Test
    void theIdReturnedByRecordIsTheOneTheRunShows() {
        Sampling sampling = runningSampling();

        IncidentId incidentId = incidents.record(sampling.expeditionId(), new IncidentDescription("storm on site"), sampling.activityId());

        assertEquals(incidentId, runs.find(sampling.expeditionId()).orElseThrow().incidents().getFirst().id());
    }

    @Test
    void anIncidentOnAnUnknownActivityAppendsNothing() {
        Sampling sampling = runningSampling();
        int mark = eventMark();

        assertThrows(
                edu.itba.fieldops.domain.tracking.InvalidActivityExecution.class,
                () -> incidents.record(sampling.expeditionId(), new IncidentDescription("storm"), itinerary.nextActivityId())
        );

        assertEquals(List.of(), eventsSince(mark));
    }

    @Test
    void acceptingAProposalAppendsReplanDecidedAccepted() {
        Sampling sampling = runningSampling();
        clock.set(at(2));
        incidents.record(sampling.expeditionId(), new IncidentDescription("storm on site"), sampling.activityId());
        ProposalSnapshot proposal = onlyProposalOf(sampling.expeditionId());
        clock.set(at(3));
        int mark = eventMark();

        proposalReview.accept(proposal.id(), sampling.responsible());

        assertEquals(
                List.of(new ReplanDecided(sampling.expeditionId(), at(3), proposal.id(), ReplanProposal.Decision.ACCEPTED, sampling.responsible())),
                eventsSince(mark)
        );
    }

    @Test
    void rejectingAProposalAppendsReplanDecidedRejected() {
        Sampling sampling = runningSampling();
        clock.set(at(2));
        incidents.record(sampling.expeditionId(), new IncidentDescription("storm on site"), sampling.activityId());
        ProposalSnapshot proposal = onlyProposalOf(sampling.expeditionId());
        clock.set(at(3));
        int mark = eventMark();

        proposalReview.reject(proposal.id(), sampling.responsible());

        assertEquals(
                List.of(new ReplanDecided(sampling.expeditionId(), at(3), proposal.id(), ReplanProposal.Decision.REJECTED, sampling.responsible())),
                eventsSince(mark)
        );
    }

    @Test
    void aDecisionByAStrangerAppendsNothing() {
        Sampling sampling = runningSampling();
        incidents.record(sampling.expeditionId(), new IncidentDescription("storm on site"), sampling.activityId());
        ProposalSnapshot proposal = onlyProposalOf(sampling.expeditionId());
        PersonId stranger = certifiedPerson("Stranger", certification());
        int mark = eventMark();

        assertThrows(edu.itba.fieldops.domain.shared.InvalidValue.class, () -> proposalReview.accept(proposal.id(), stranger));

        assertEquals(List.of(), eventsSince(mark));
    }

    @Test
    void acceptingAProposalKeepsItsPlanAsADraftRevision() {
        Sampling sampling = runningSampling();
        incidents.record(sampling.expeditionId(), new IncidentDescription("storm on site"), sampling.activityId());
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
        incidents.record(sampling.expeditionId(), new IncidentDescription("equipment failure"), sampling.activityId());
        ProposalSnapshot proposal = onlyProposalOf(sampling.expeditionId());

        proposalReview.reject(proposal.id(), sampling.responsible());

        assertAll(
                () -> assertEquals(ReplanProposal.Decision.REJECTED, onlyProposalOf(sampling.expeditionId()).decision()),
                () -> assertTrue(plans.find(proposal.suggested().id()).isEmpty()),
                () -> assertEquals(ExpeditionStatus.APPROVED, consult.of(sampling.expeditionId()).status())
        );
    }

    @Test
    void delayingADraftShiftsItInPlace() {
        Sampling sampling = samplingPlan();

        replan.delay(sampling.expeditionId(), sampling.activityId(), new Delay(Duration.ofHours(1)));

        PlanSnapshot plan = consult.of(sampling.expeditionId());
        assertAll(
                () -> assertEquals(ExpeditionStatus.DRAFT, plan.status()),
                () -> assertEquals(hours(1, 5), plan.itinerary().getFirst().activities().getFirst().window())
        );
    }

    @Test
    void revisingAnApprovedPlanCreatesTheNextDraftVersion() {
        Sampling sampling = approvedSampling();

        ExpeditionId revision = replan.revise(sampling.expeditionId());

        assertAll(
                () -> assertEquals(ExpeditionStatus.APPROVED, consult.of(sampling.expeditionId()).status()),
                () -> assertEquals(ExpeditionStatus.DRAFT, consult.of(revision).status()),
                () -> assertEquals(2, consult.of(revision).version())
        );
    }

    @Test
    void anUnavailableVehicleIsReplacedInTheRevision() {
        PersonId ada = personnel.registerPerson(new PersonName("Ada"), List.of(), Availability.always());
        VehicleId boat = equipment.registerVehicle(new Capacity(4), Availability.always());
        VehicleId spare = equipment.registerVehicle(new Capacity(4), Availability.always());
        Crossing crossing = crossing(ada, boat);
        review.submit(crossing.expeditionId());
        approval.approve(crossing.expeditionId());
        equipment.changeAvailability(boat, new Availability(List.of()));
        ExpeditionId revision = replan.revise(crossing.expeditionId());

        replan.replaceUnavailable(revision);

        assertEquals(List.of(new VehicleAssignment(crossing.activityId(), spare)), consult.of(revision).assignments());
    }

    @Test
    void anIncidentAfterAnApprovedReplanProposesOnTheRevisionInForce() {
        Sampling sampling = runningSampling();
        clock.set(at(1));
        incidents.record(sampling.expeditionId(), new IncidentDescription("storm on site"), sampling.activityId());
        ExpeditionId revision = onlyProposalOf(sampling.expeditionId()).suggested().id();
        proposalReview.accept(onlyProposalOf(sampling.expeditionId()).id(), sampling.responsible());
        review.submit(revision);
        approval.approve(revision);
        clock.set(at(2));

        IncidentId incidentId = incidents.record(sampling.expeditionId(), new IncidentDescription("trail flooded"), sampling.activityId());

        ProposalSnapshot proposal = onlyProposalOf(revision);
        assertAll(
                () -> assertEquals(
                        new Incident(incidentId, new IncidentDescription("trail flooded"), at(2), Optional.of(sampling.activityId())),
                        proposal.incident()
                ),
                () -> assertEquals(ReplanProposal.Decision.PENDING, proposal.decision()),
                () -> assertEquals(ExpeditionStatus.APPROVED, consult.of(revision).status())
        );
    }

    @Test
    void consultsAProposalByItsId() {
        Sampling sampling = runningSampling();
        incidents.record(sampling.expeditionId(), new IncidentDescription("storm on site"), sampling.activityId());
        ProposalSnapshot listed = onlyProposalOf(sampling.expeditionId());

        ProposalSnapshot found = proposalReview.of(listed.id());

        assertAll(
                () -> assertEquals(listed.incident(), found.incident()),
                () -> assertEquals(listed.suggested().id(), found.suggested().id())
        );
    }

    @Test
    void rejectsConsultingProposalsOfUnknownPlansOrIds() {
        assertAll(
                () -> assertThrows(UnknownResource.class, () -> proposalReview.of(new ExpeditionId(UUID.randomUUID()), FIRST_PAGE)),
                () -> assertThrows(UnknownResource.class, () -> proposalReview.of(new ProposalId(UUID.randomUUID())))
        );
    }

    private ProposalSnapshot onlyProposalOf(ExpeditionId expeditionId) {
        List<ProposalSnapshot> found = proposalReview.of(expeditionId, FIRST_PAGE).items();
        assertEquals(1, found.size());
        return found.getFirst();
    }
}
