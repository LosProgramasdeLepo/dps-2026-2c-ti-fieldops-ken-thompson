package edu.itba.fieldops.domain.expedition.usecase;

import edu.itba.fieldops.domain.expedition.ReplanProposal;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.ProposalId;
import edu.itba.fieldops.domain.tracking.Incident;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record ProposalSnapshot(
        ProposalId id,
        ExpeditionId originalId,
        Incident incident,
        ReplanProposal.Decision decision,
        Optional<PersonId> decidedBy,
        Optional<Instant> decidedAt,
        PlanSnapshot suggested
) {
    public ProposalSnapshot {
        Objects.requireNonNull(id, "proposal id");
        Objects.requireNonNull(originalId, "original expedition id");
        Objects.requireNonNull(incident, "incident");
        Objects.requireNonNull(decision, "decision");
        Objects.requireNonNull(decidedBy, "decided by");
        Objects.requireNonNull(decidedAt, "decided at");
        Objects.requireNonNull(suggested, "suggested plan");
    }

    public static ProposalSnapshot of(ReplanProposal proposal) {
        return new ProposalSnapshot(
                proposal.id(),
                proposal.originalId(),
                proposal.incident(),
                proposal.decision(),
                proposal.decidedBy(),
                proposal.decidedAt(),
                PlanSnapshot.of(proposal.suggested())
        );
    }
}
