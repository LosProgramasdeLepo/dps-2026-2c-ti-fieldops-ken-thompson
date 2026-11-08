package edu.itba.fieldops.domain.event;

import edu.itba.fieldops.domain.expedition.ReplanProposal;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.ProposalId;
import edu.itba.fieldops.domain.shared.InvalidValue;

import java.time.Instant;
import java.util.Objects;

public record ReplanDecided(
        ExpeditionId expeditionId,
        Instant occurredAt,
        ProposalId proposalId,
        ReplanProposal.Decision decision,
        PersonId decidedBy
) implements DomainEvent {
    public ReplanDecided {
        Objects.requireNonNull(expeditionId, "expedition id");
        Objects.requireNonNull(occurredAt, "occurred at");
        Objects.requireNonNull(proposalId, "proposal id");
        Objects.requireNonNull(decision, "decision");
        Objects.requireNonNull(decidedBy, "responsible");
        if (decision == ReplanProposal.Decision.PENDING) {
            throw new InvalidValue("a decision event records an acceptance or a rejection");
        }
    }
}
