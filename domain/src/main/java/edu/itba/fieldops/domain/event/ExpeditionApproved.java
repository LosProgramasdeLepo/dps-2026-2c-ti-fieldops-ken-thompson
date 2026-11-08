package edu.itba.fieldops.domain.event;

import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.shared.InvalidValue;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record ExpeditionApproved(
        ExpeditionId expeditionId,
        Instant occurredAt,
        int version,
        Optional<ExpeditionId> superseded
) implements DomainEvent {
    public ExpeditionApproved {
        Objects.requireNonNull(expeditionId, "expedition id");
        Objects.requireNonNull(occurredAt, "occurred at");
        Objects.requireNonNull(superseded, "superseded plan");
        if (version < 1) {
            throw new InvalidValue("version must be positive: " + version);
        }
    }
}
