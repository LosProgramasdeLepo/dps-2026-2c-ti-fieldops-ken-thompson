package edu.itba.fieldops.adapters.mongo.tracking;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.IncidentId;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.domain.tracking.IncidentDescription;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public record IncidentDocument(String id, String description, Instant at, String activityId) {
    public static IncidentDocument of(Incident incident) {
        return new IncidentDocument(
                incident.id().value().toString(),
                incident.description().text(),
                incident.at(),
                incident.activityId().map(id -> id.value().toString()).orElse(null)
        );
    }

    /**
     * Documents written before incidents had an identity carry no id. They get a deterministic one derived from
     * {@code legacySeed}, so every read of the same document yields the same {@link IncidentId}.
     */
    public Incident toDomain(String legacySeed) {
        return new Incident(
                new IncidentId(id != null ? UUID.fromString(id) : UUID.nameUUIDFromBytes(legacySeed.getBytes(StandardCharsets.UTF_8))),
                new IncidentDescription(description),
                at,
                Optional.ofNullable(activityId).map(UUID::fromString).map(ActivityId::new)
        );
    }
}
