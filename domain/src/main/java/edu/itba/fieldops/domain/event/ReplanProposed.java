package edu.itba.fieldops.domain.event;

import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.IncidentId;
import edu.itba.fieldops.domain.identity.ProposalId;

import java.time.Instant;
import java.util.Objects;

public record ReplanProposed(
        ExpeditionId expeditionId,
        Instant occurredAt,
        ProposalId proposalId,
        ExpeditionId suggested,
        IncidentId incidentId
) implements DomainEvent {
    public ReplanProposed {
        Objects.requireNonNull(expeditionId, "expedition id");
        Objects.requireNonNull(occurredAt, "occurred at");
        Objects.requireNonNull(proposalId, "proposal id");
        Objects.requireNonNull(suggested, "suggested plan");
        Objects.requireNonNull(incidentId, "incident id");
    }
}
