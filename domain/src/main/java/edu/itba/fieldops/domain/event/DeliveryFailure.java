package edu.itba.fieldops.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record DeliveryFailure(Instant at, String error, Optional<Instant> retryAt) {
    public DeliveryFailure {
        Objects.requireNonNull(at, "failed at");
        Objects.requireNonNull(error, "error");
        Objects.requireNonNull(retryAt, "retry at");
    }
}
