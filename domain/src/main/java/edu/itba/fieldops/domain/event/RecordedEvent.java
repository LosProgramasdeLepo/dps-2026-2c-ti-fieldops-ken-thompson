package edu.itba.fieldops.domain.event;

import edu.itba.fieldops.domain.identity.EventId;
import edu.itba.fieldops.domain.shared.InvalidValue;

import java.time.Instant;
import java.util.Objects;

public record RecordedEvent(EventId id, long sequence, Instant recordedAt, DomainEvent event) {
    public RecordedEvent {
        Objects.requireNonNull(id, "event id");
        Objects.requireNonNull(recordedAt, "recorded at");
        Objects.requireNonNull(event, "event");
        if (sequence < 1) {
            throw new InvalidValue("sequence must be positive: " + sequence);
        }
    }
}
