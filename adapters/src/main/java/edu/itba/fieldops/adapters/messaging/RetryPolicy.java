package edu.itba.fieldops.adapters.messaging;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class RetryPolicy {
    private final int maxAttempts;
    private final Duration base;
    private final Duration max;

    public RetryPolicy(int maxAttempts, Duration base, Duration max) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("max attempts must be at least 1: " + maxAttempts);
        }
        this.maxAttempts = maxAttempts;
        this.base = Objects.requireNonNull(base, "backoff base");
        this.max = Objects.requireNonNull(max, "backoff max");
        if (!base.isPositive() || max.compareTo(base) < 0) {
            throw new IllegalArgumentException("backoff must satisfy 0 < base <= max: " + base + ", " + max);
        }
    }

    public Optional<Instant> nextAttemptAfter(int attempt, Instant failedAt) {
        return attempt < maxAttempts ? Optional.of(failedAt.plus(delayAfter(attempt))) : Optional.empty();
    }

    Duration delayAfter(int attempt) {
        Duration half = max.dividedBy(2);
        Duration delay = base;
        for (int doubled = 1; doubled < attempt; doubled++) {
            if (delay.compareTo(half) > 0) {
                return max;
            }
            delay = delay.multipliedBy(2);
        }
        return delay;
    }
}
