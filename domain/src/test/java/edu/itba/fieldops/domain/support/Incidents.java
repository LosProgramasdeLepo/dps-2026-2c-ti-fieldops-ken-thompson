package edu.itba.fieldops.domain.support;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.IncidentId;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.domain.tracking.IncidentDescription;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class Incidents {
    private Incidents() {
    }

    public static Incident of(IncidentDescription description, Instant at) {
        return new Incident(newId(), description, at, Optional.empty());
    }

    public static Incident affecting(ActivityId activityId, IncidentDescription description, Instant at) {
        return new Incident(newId(), description, at, Optional.of(activityId));
    }

    public static IncidentId newId() {
        return new IncidentId(UUID.randomUUID());
    }
}
