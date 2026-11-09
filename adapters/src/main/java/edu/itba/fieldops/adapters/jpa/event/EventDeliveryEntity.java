package edu.itba.fieldops.adapters.jpa.event;

import edu.itba.fieldops.adapters.event.DeliveryProgress;
import edu.itba.fieldops.domain.event.DeliveryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Optional;

@Entity
@Table(name = "event_deliveries")
public class EventDeliveryEntity {
    @EmbeddedId
    private DeliveryKey id;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    protected EventDeliveryEntity() {
    }

    EventDeliveryEntity(DeliveryKey id, DeliveryProgress progress) {
        this.id = id;
        advanceTo(progress);
    }

    final void advanceTo(DeliveryProgress progress) {
        this.status = progress.status().name();
        this.attempts = progress.attempts();
        this.nextAttemptAt = progress.nextAttemptAt().orElse(null);
        this.lastAttemptAt = progress.lastAttemptAt().orElse(null);
        this.lastError = progress.lastError().orElse(null);
        this.deliveredAt = progress.deliveredAt().orElse(null);
    }

    DeliveryProgress progress() {
        return new DeliveryProgress(
                DeliveryStatus.valueOf(status),
                attempts,
                Optional.ofNullable(nextAttemptAt),
                Optional.ofNullable(lastAttemptAt),
                Optional.ofNullable(lastError),
                Optional.ofNullable(deliveredAt)
        );
    }

    DeliveryKey id() {
        return id;
    }
}
