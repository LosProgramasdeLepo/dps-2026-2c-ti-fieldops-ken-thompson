package edu.itba.fieldops.api.json;

import edu.itba.fieldops.domain.event.Delivery;

import java.time.Instant;

public record DeliveryResponse(String consumer, String status, int attempts, Instant lastAttemptAt, String lastError) {
    public static DeliveryResponse of(Delivery delivery) {
        return new DeliveryResponse(
                delivery.consumer(),
                delivery.status().name(),
                delivery.attempts(),
                delivery.lastAttemptAt().orElse(null),
                delivery.lastError().orElse(null)
        );
    }
}
