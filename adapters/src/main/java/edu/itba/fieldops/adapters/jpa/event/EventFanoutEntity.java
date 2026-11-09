package edu.itba.fieldops.adapters.jpa.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "event_fanout")
public class EventFanoutEntity {
    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "dispatched_at", nullable = false)
    private Instant dispatchedAt;

    protected EventFanoutEntity() {
    }

    EventFanoutEntity(UUID eventId, Instant dispatchedAt) {
        this.eventId = eventId;
        this.dispatchedAt = dispatchedAt;
    }
}
