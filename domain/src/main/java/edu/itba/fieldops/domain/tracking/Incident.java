package edu.itba.fieldops.domain.tracking;

import edu.itba.fieldops.domain.identity.ActivityId;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record Incident(IncidentDescription description, Instant at, Optional<ActivityId> activityId) {
    public Incident {
        Objects.requireNonNull(description, "incident");
        Objects.requireNonNull(at, "incident time");
        Objects.requireNonNull(activityId, "affected activity");
    }

    public static Incident of(IncidentDescription description, Instant at) {
        return new Incident(description, at, Optional.empty());
    }

    public static Incident affecting(ActivityId activityId, IncidentDescription description, Instant at) {
        return new Incident(description, at, Optional.of(activityId));
    }
}
