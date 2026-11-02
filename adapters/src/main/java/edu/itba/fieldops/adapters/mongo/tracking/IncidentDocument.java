package edu.itba.fieldops.adapters.mongo.tracking;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.domain.tracking.IncidentDescription;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public record IncidentDocument(String description, Instant at, String activityId) {
    public static IncidentDocument of(Incident incident) {
        return new IncidentDocument(
                incident.description().text(),
                incident.at(),
                incident.activityId().map(id -> id.value().toString()).orElse(null)
        );
    }

    public Incident toDomain() {
        return new Incident(
                new IncidentDescription(description),
                at,
                Optional.ofNullable(activityId).map(UUID::fromString).map(ActivityId::new)
        );
    }
}
