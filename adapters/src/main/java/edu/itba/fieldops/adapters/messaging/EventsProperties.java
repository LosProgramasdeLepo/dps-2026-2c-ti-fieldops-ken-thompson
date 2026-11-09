package edu.itba.fieldops.adapters.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties("fieldops.events")
public record EventsProperties(
        @DefaultValue("50") int batchSize,
        @DefaultValue("5") int maxAttempts,
        @DefaultValue("1s") Duration backoffBase,
        @DefaultValue("1m") Duration backoffMax
) {
    public EventsProperties {
        if (batchSize < 1) {
            throw new IllegalArgumentException("batch size must be at least 1: " + batchSize);
        }
    }

    RetryPolicy retryPolicy() {
        return new RetryPolicy(maxAttempts, backoffBase, backoffMax);
    }
}
