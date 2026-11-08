package edu.itba.fieldops.domain.tracking;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.IncidentId;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record Incident(IncidentId id, IncidentDescription description, Instant at, Optional<ActivityId> activityId) {
    public Incident {
        Objects.requireNonNull(id, "incident id");
        Objects.requireNonNull(description, "incident");
        Objects.requireNonNull(at, "incident time");
        Objects.requireNonNull(activityId, "affected activity");
    }

    public static Incident of(IncidentReport report, Instant at) {
        Objects.requireNonNull(report, "incident report");
        return new Incident(report.id(), report.description(), at, report.activityId());
    }
}
