package edu.itba.fieldops.domain.event;

import edu.itba.fieldops.domain.shared.InvalidValue;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record Delivery(
        String consumer,
        DeliveryStatus status,
        int attempts,
        Optional<Instant> lastAttemptAt,
        Optional<String> lastError
) {
    public Delivery {
        Objects.requireNonNull(consumer, "consumer");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(lastAttemptAt, "last attempt");
        Objects.requireNonNull(lastError, "last error");
        if (attempts < 0) {
            throw new InvalidValue("attempts must not be negative: " + attempts);
        }
    }

    public void requireRetriable() {
        if (status != DeliveryStatus.FAILED) {
            throw new InvalidDeliveryTransition(consumer, status);
        }
    }
}
