package edu.itba.fieldops.domain.event;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;

import java.time.Instant;
import java.util.Objects;

public record ActivityStarted(ExpeditionId expeditionId, Instant occurredAt, ActivityId activityId) implements DomainEvent {
    public ActivityStarted {
        Objects.requireNonNull(expeditionId, "expedition id");
        Objects.requireNonNull(occurredAt, "occurred at");
        Objects.requireNonNull(activityId, "activity id");
    }
}
