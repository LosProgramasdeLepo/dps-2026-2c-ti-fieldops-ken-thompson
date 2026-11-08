package edu.itba.fieldops.domain.event;

import edu.itba.fieldops.domain.identity.ExpeditionId;

import java.time.Instant;
import java.util.Objects;

public record RunStarted(ExpeditionId expeditionId, Instant occurredAt) implements DomainEvent {
    public RunStarted {
        Objects.requireNonNull(expeditionId, "expedition id");
        Objects.requireNonNull(occurredAt, "occurred at");
    }
}
