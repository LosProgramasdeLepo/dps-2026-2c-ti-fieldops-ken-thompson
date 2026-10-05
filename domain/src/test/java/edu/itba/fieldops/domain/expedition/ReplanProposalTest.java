package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.ProposalId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
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
        PersonId ada = new PersonId(UUID.randomUUID());
        ReplanProposal proposal = pending();

        proposal.accept(ada, DAY);

        assertEquals(ReplanProposal.Decision.ACCEPTED, proposal.decision());
        assertEquals(Optional.of(ada), proposal.decidedBy());
        assertEquals(Optional.of(DAY), proposal.decidedAt());
    }

    @Test
    void rejectRecordsTheResponsible() {
        PersonId ada = new PersonId(UUID.randomUUID());
        ReplanProposal proposal = pending();

        proposal.reject(ada, DAY);

        assertEquals(ReplanProposal.Decision.REJECTED, proposal.decision());
        assertEquals(Optional.of(ada), proposal.decidedBy());
    }

    @Test
    void cannotAcceptTwice() {
        ReplanProposal proposal = pending();
        proposal.accept(new PersonId(UUID.randomUUID()), DAY);

        assertThrows(
                InvalidValue.class,
                () -> proposal.accept(new PersonId(UUID.randomUUID()), DAY.plus(Duration.ofHours(1)))
        );
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

    private static ReplanProposal pending() {
        return new ReplanProposal(new ProposalId(UUID.randomUUID()), incident(), revision());
    }

    private static Incident incident() {
        return Incident.affecting(new ActivityId(UUID.randomUUID()), "storm on site", DAY);
    }

    private static Expedition revision() {
        Expedition approved = draft();
        approved.addActivity(Activity.transit()
                .named(new ActivityId(UUID.randomUUID()), "Approach")
                .estimated(Duration.ofHours(2), RiskLevel.LOW)
                .in(new WorkZone("Delta"), new TimePeriod(DAY, DAY.plus(Duration.ofHours(2))))
                .build());
        approved.submitForReview();
        approved.markApproved();
        return approved.reviseAsDraft(new ExpeditionId(UUID.randomUUID()));
    }

    private static Expedition draft() {
        return Expedition.draft(
                new ExpeditionId(UUID.randomUUID()),
                new ExpeditionCharter(
                        List.of(new Objective("Map wetland")),
                        new TimePeriod(DAY, DAY.plus(Duration.ofDays(5))),
                        List.of(new WorkZone("Delta")),
                        List.of(new PersonId(UUID.randomUUID())),
                        List.of(new Restriction("Daylight only"))
                )
        );
    }
}
