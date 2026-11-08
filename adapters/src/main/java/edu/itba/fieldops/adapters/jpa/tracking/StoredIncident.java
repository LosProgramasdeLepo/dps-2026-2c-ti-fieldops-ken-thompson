package edu.itba.fieldops.adapters.jpa.tracking;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.IncidentId;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.domain.tracking.IncidentDescription;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Embeddable
public record StoredIncident(
        @Column(name = "id", nullable = false) UUID id,
        @Column(name = "description", nullable = false) String description,
        @Column(name = "occurred_at", nullable = false) Instant occurredAt,
        @Column(name = "activity_id") UUID activityId
) {
    public static StoredIncident of(Incident incident) {
        return new StoredIncident(
                incident.id().value(),
                incident.description().text(),
                incident.at(),
                incident.activityId().map(ActivityId::value).orElse(null)
        );
    }

    public Incident toDomain() {
        return new Incident(new IncidentId(id), new IncidentDescription(description), occurredAt, Optional.ofNullable(activityId).map(ActivityId::new));
    }
}
