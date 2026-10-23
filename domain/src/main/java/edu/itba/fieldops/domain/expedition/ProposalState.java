package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.ProposalId;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.tracking.Incident;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record ProposalState(
        ProposalId id,
        ExpeditionId originalId,
        ExpeditionId suggestedId,
        Incident incident,
        ReplanProposal.Decision decision,
        Optional<PersonId> decidedBy,
        Optional<Instant> decidedAt
) {
    public ProposalState {
        Objects.requireNonNull(id, "proposal id");
        Objects.requireNonNull(originalId, "original expedition");
        Objects.requireNonNull(suggestedId, "suggested plan");
        Objects.requireNonNull(incident, "incident");
        Objects.requireNonNull(decision, "decision");
        Objects.requireNonNull(decidedBy, "responsible");
        Objects.requireNonNull(decidedAt, "decision time");
        if (decision == ReplanProposal.Decision.PENDING) {
            if (decidedBy.isPresent() || decidedAt.isPresent()) {
                throw new InvalidValue("a pending proposal has no decision");
            }
        } else if (decidedBy.isEmpty() || decidedAt.isEmpty()) {
            throw new InvalidValue("a decision records who and when");
        }
    }
}
