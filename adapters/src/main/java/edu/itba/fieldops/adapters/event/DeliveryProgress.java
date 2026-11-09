package edu.itba.fieldops.adapters.event;

import edu.itba.fieldops.domain.event.Delivery;
import edu.itba.fieldops.domain.event.DeliveryFailure;
import edu.itba.fieldops.domain.event.DeliveryStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record DeliveryProgress(
        DeliveryStatus status,
        int attempts,
        Optional<Instant> nextAttemptAt,
        Optional<Instant> lastAttemptAt,
        Optional<String> lastError,
        Optional<Instant> deliveredAt
) {
    public DeliveryProgress {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(nextAttemptAt, "next attempt");
        Objects.requireNonNull(lastAttemptAt, "last attempt");
        Objects.requireNonNull(lastError, "last error");
        Objects.requireNonNull(deliveredAt, "delivered at");
    }

    public static DeliveryProgress pendingFrom(Instant at) {
        return new DeliveryProgress(
                DeliveryStatus.PENDING, 0, Optional.of(at), Optional.empty(), Optional.empty(), Optional.empty()
        );
    }

    public DeliveryProgress delivered(Instant at) {
        return new DeliveryProgress(
                DeliveryStatus.DELIVERED, attempts + 1, Optional.empty(), Optional.of(at), lastError, Optional.of(at)
        );
    }

    public DeliveryProgress failed(DeliveryFailure failure) {
        return new DeliveryProgress(
                failure.retryAt().isPresent() ? DeliveryStatus.RETRYING : DeliveryStatus.FAILED,
                attempts + 1,
                failure.retryAt(),
                Optional.of(failure.at()),
                Optional.of(failure.error()),
                Optional.empty()
        );
    }

    public DeliveryProgress retried(Instant at) {
        return new DeliveryProgress(DeliveryStatus.PENDING, 0, Optional.of(at), lastAttemptAt, lastError, Optional.empty());
    }

    public boolean isDue(Instant now) {
        return nextAttemptAt.filter(next -> !next.isAfter(now)).isPresent();
    }

    public Delivery toDelivery(String consumer) {
        return new Delivery(consumer, status, attempts, lastAttemptAt, lastError);
    }
}
