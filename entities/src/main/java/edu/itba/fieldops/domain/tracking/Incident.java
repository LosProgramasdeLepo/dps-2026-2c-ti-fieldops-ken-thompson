package edu.itba.fieldops.domain.tracking;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.shared.Texts;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record Incident(String description, Instant at, Optional<ActivityId> activityId) {
    public Incident {
        description = Texts.required(description, "incident");
        Objects.requireNonNull(at, "incident time");
        Objects.requireNonNull(activityId, "affected activity");
    }

    public static Incident of(String description, Instant at) {
        return new Incident(description, at, Optional.empty());
    }

    public static Incident affecting(ActivityId activityId, String description, Instant at) {
        return new Incident(description, at, Optional.of(activityId));
    }
}
