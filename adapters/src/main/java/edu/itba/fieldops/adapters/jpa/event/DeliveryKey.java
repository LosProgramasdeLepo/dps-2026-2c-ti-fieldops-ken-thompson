package edu.itba.fieldops.adapters.jpa.event;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
public record DeliveryKey(
        @Column(name = "event_id", nullable = false) UUID eventId,
        @Column(nullable = false) String consumer
) implements Serializable {
}
