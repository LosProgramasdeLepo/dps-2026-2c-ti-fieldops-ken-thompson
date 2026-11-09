package edu.itba.fieldops.domain.event;

import edu.itba.fieldops.domain.shared.InvalidValue;

import java.util.Objects;

public record PendingDelivery(RecordedEvent event, String consumer, int attempts) {
    public PendingDelivery {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(consumer, "consumer");
        if (attempts < 0) {
            throw new InvalidValue("attempts must not be negative: " + attempts);
        }
    }
}
