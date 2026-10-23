package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.ProposalId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.support.StoredPeople;
import edu.itba.fieldops.domain.support.StoredPlans;
import edu.itba.fieldops.domain.tracking.Incident;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReplanProposalTest {
    private static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");

    @Test
    void acceptRecordsTheResponsible() {
        Expedition original = approved();
        PersonId ada = original.charter().responsibles().getFirst();
        ReplanProposal proposal = proposalFor(original);

        proposal.accept(original, original.forReplan(ada), DAY);

        assertEquals(ReplanProposal.Decision.ACCEPTED, proposal.decision());
        assertEquals(Optional.of(ada), proposal.decidedBy());
        assertEquals(Optional.of(DAY), proposal.decidedAt());
        assertEquals(ExpeditionStatus.APPROVED, original.status());
    }

    @Test
    void rejectRecordsTheResponsible() {
        Expedition original = approved();
        PersonId ada = original.charter().responsibles().getFirst();
        ReplanProposal proposal = proposalFor(original);

        proposal.reject(original, original.forReplan(ada), DAY);

        assertEquals(ReplanProposal.Decision.REJECTED, proposal.decision());
        assertEquals(Optional.of(ada), proposal.decidedBy());
        assertEquals(ExpeditionStatus.APPROVED, original.status());
    }

    @Test
    void cannotAcceptTwice() {
        Expedition original = approved();
        ReplanProposal proposal = proposalFor(original);
        Responsible ada = original.forReplan(original.charter().responsibles().getFirst());
        proposal.accept(original, ada, DAY);

        assertThrows(
                InvalidValue.class,
                () -> proposal.accept(original, ada, DAY.plus(Duration.ofHours(1)))
        );
    }

    @Test
    void aStrangerCannotDecideAProposal() {
        Expedition original = approved();

        InvalidValue rejected = assertThrows(
                InvalidValue.class,
                () -> original.forReplan(new PersonId(UUID.randomUUID()))
        );

        assertEquals("replan must be decided by a responsible", rejected.getMessage());
    }

    @Test
    void aResponsibleOfAnotherPlanCannotAccept() {
        PersonId lead = new PersonId(UUID.randomUUID());
        Expedition original = approved(lead);
        Expedition other = approved(lead);
        ReplanProposal proposal = proposalFor(original);

        InvalidValue rejected = assertThrows(
                InvalidValue.class,
                () -> proposal.accept(original, other.forReplan(lead), DAY)
        );

        assertEquals("replan must be decided by a responsible", rejected.getMessage());
        assertEquals(ReplanProposal.Decision.PENDING, proposal.decision());
    }

    @Test
    void anotherPlanCannotStandInForTheOriginal() {
        PersonId lead = new PersonId(UUID.randomUUID());
        ReplanProposal proposal = proposalFor(approved(lead));
        Expedition other = approved(lead);

        InvalidValue rejected = assertThrows(
                InvalidValue.class,
                () -> proposal.accept(other, other.forReplan(lead), DAY)
        );

        assertEquals("replan must be decided on the plan it revises", rejected.getMessage());
        assertEquals(ReplanProposal.Decision.PENDING, proposal.decision());
    }

    @Test
    void cannotDecideBeforeTheIncident() {
        Expedition original = approved();
        ReplanProposal proposal = proposalFor(original);
        Responsible ada = original.forReplan(original.charter().responsibles().getFirst());

        InvalidValue rejected = assertThrows(
                InvalidValue.class,
                () -> proposal.reject(original, ada, DAY.minus(Duration.ofHours(1)))
        );

        assertEquals("replan must be decided after its incident", rejected.getMessage());
        assertEquals(ReplanProposal.Decision.PENDING, proposal.decision());
    }

    @Test
    void cannotAcceptWhenTheOriginalIsNoLongerApproved() {
        Expedition original = approved();
        ReplanProposal proposal = proposalFor(original);
        Responsible ada = original.forReplan(original.charter().responsibles().getFirst());
        original.markSuperseded();

        InvalidExpeditionTransition rejected = assertThrows(
                InvalidExpeditionTransition.class,
                () -> proposal.accept(original, ada, DAY)
        );

        assertEquals("cannot accept replan while expedition is SUPERSEDED", rejected.getMessage());
        assertEquals(ReplanProposal.Decision.PENDING, proposal.decision());
    }

    @Test
    void aStoredDecisionIsRestoredAfterTheOriginalIsSuperseded() {
        Expedition original = approved();
        PersonId ada = original.charter().responsibles().getFirst();
        Expedition revision = original.reviseAsDraft(new ExpeditionId(UUID.randomUUID()));
        original.markSuperseded();

        ReplanProposal restored = ReplanProposal.restore(
                new ProposalState(
                        new ProposalId(UUID.randomUUID()),
                        original.id(),
                        revision.id(),
                        incident(),
                        ReplanProposal.Decision.ACCEPTED,
                        Optional.of(ada),
                        Optional.of(DAY)
                ),
                revision
        );

        assertEquals(ReplanProposal.Decision.ACCEPTED, restored.decision());
        assertEquals(original.id(), restored.originalId());
        assertEquals(Optional.of(ada), restored.decidedBy());
        assertEquals(Optional.of(DAY), restored.decidedAt());
    }

    @Test
    void aPendingProposalDoesNotCarryADecision() {
        InvalidValue rejected = assertThrows(InvalidValue.class, () -> new ProposalState(
                new ProposalId(UUID.randomUUID()),
                new ExpeditionId(UUID.randomUUID()),
                new ExpeditionId(UUID.randomUUID()),
                incident(),
                ReplanProposal.Decision.PENDING,
                Optional.of(new PersonId(UUID.randomUUID())),
                Optional.of(DAY)
        ));

        assertEquals("a pending proposal has no decision", rejected.getMessage());
    }

    @Test
    void aDecisionRecordsWhoAndWhen() {
        InvalidValue rejected = assertThrows(InvalidValue.class, () -> new ProposalState(
                new ProposalId(UUID.randomUUID()),
                new ExpeditionId(UUID.randomUUID()),
                new ExpeditionId(UUID.randomUUID()),
                incident(),
                ReplanProposal.Decision.REJECTED,
                Optional.empty(),
                Optional.empty()
        ));

        assertEquals("a decision records who and when", rejected.getMessage());
    }

    @Test
    void incidentWithoutActivityIsRejected() {
        Incident general = Incident.of("storm", DAY);
        Expedition revision = revision();

        assertThrows(InvalidValue.class, () -> new ReplanProposal(new ProposalId(UUID.randomUUID()), general, revision));
    }

    @Test
    void theSuggestedPlanMustReviseTheOriginal() {
        Expedition original = draft();

        assertThrows(InvalidValue.class, () -> new ReplanProposal(new ProposalId(UUID.randomUUID()), incident(), original));
    }

    @Test
    void theOriginalIsThePlanTheSuggestionRevises() {
        Expedition revision = revision();

        ReplanProposal proposal = new ReplanProposal(new ProposalId(UUID.randomUUID()), incident(), revision);

        assertEquals(revision.supersedes().orElseThrow(), proposal.originalId());
    }

    private static ReplanProposal proposalFor(Expedition original) {
        return new ReplanProposal(new ProposalId(UUID.randomUUID()), incident(), original.reviseAsDraft(new ExpeditionId(UUID.randomUUID())));
    }

    private static Incident incident() {
        return Incident.affecting(new ActivityId(UUID.randomUUID()), "storm on site", DAY);
    }

    private static Expedition revision() {
        return approved().reviseAsDraft(new ExpeditionId(UUID.randomUUID()));
    }

    private static Expedition approved() {
        return approved(new PersonId(UUID.randomUUID()));
    }

    private static Expedition approved(PersonId lead) {
        Expedition expedition = draft(lead);
        expedition.addActivity(Activity.transit()
                .named(new ActivityId(UUID.randomUUID()), "Approach")
                .estimated(Duration.ofHours(2), RiskLevel.LOW)
                .in(new WorkZone("Delta"), new TimePeriod(DAY, DAY.plus(Duration.ofHours(2))))
                .build());
        expedition.submitForReview();
        return StoredPlans.approved(expedition);
    }

    private static Expedition draft() {
        return draft(new PersonId(UUID.randomUUID()));
    }

    private static Expedition draft(PersonId lead) {
        return Expedition.draft(
                new ExpeditionId(UUID.randomUUID()),
                new ExpeditionCharter(
                        List.of(new Objective("Map wetland")),
                        new TimePeriod(DAY, DAY.plus(Duration.ofDays(5))),
                        List.of(new WorkZone("Delta")),
                        List.of(lead),
                        List.of(new Restriction("Daylight only"))
                ),
                StoredPeople.of(new Person(lead, "Ada", List.of(), Availability.always()))
        );
    }
}
