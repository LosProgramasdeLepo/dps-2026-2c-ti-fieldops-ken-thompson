package edu.itba.fieldops.adapters.jpa.tracking;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.tracking.Incident;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Embeddable
public record StoredIncident(
        @Column(name = "description", nullable = false) String description,
        @Column(name = "occurred_at", nullable = false) Instant occurredAt,
        @Column(name = "activity_id") UUID activityId
) {
    public static StoredIncident of(Incident incident) {
        return new StoredIncident(
                incident.description(),
                incident.at(),
                incident.activityId().map(ActivityId::value).orElse(null)
        );
    }

    public Incident toDomain() {
        return new Incident(description, occurredAt, Optional.ofNullable(activityId).map(ActivityId::new));
    }
}
