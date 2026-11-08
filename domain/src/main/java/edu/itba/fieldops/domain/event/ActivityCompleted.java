package edu.itba.fieldops.domain.event;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.shared.Consumption;
import edu.itba.fieldops.domain.tracking.Outcome;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record ActivityCompleted(
        ExpeditionId expeditionId,
        Instant occurredAt,
        ActivityId activityId,
        Instant startedAt,
        Outcome result,
        Map<ConsumableId, Consumption> consumed
) implements DomainEvent {
    public ActivityCompleted {
        Objects.requireNonNull(expeditionId, "expedition id");
        Objects.requireNonNull(occurredAt, "occurred at");
        Objects.requireNonNull(activityId, "activity id");
        Objects.requireNonNull(startedAt, "started at");
        Objects.requireNonNull(result, "result");
        consumed = Map.copyOf(Objects.requireNonNull(consumed, "consumed"));
    }
}
