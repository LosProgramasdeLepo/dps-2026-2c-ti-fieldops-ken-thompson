package edu.itba.fieldops.domain.tracking;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.IncidentId;

import java.util.Objects;
import java.util.Optional;

public record IncidentReport(IncidentId id, IncidentDescription description, Optional<ActivityId> activityId) {
    public IncidentReport {
        Objects.requireNonNull(id, "incident id");
        Objects.requireNonNull(description, "incident");
        Objects.requireNonNull(activityId, "affected activity");
    }

    public static IncidentReport general(IncidentId id, IncidentDescription description) {
        return new IncidentReport(id, description, Optional.empty());
    }

    public static IncidentReport affecting(IncidentId id, ActivityId activityId, IncidentDescription description) {
        return new IncidentReport(id, description, Optional.of(Objects.requireNonNull(activityId, "affected activity")));
    }
}
