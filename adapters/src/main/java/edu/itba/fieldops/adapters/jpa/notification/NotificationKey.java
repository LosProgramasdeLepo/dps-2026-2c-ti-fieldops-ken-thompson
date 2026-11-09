package edu.itba.fieldops.adapters.jpa.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
public record NotificationKey(
        @Column(name = "event_id", nullable = false) UUID eventId,
        @Column(name = "person_id", nullable = false) UUID personId
) implements Serializable {
}
