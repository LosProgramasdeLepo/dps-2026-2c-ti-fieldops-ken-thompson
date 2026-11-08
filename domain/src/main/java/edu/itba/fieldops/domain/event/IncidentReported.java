package edu.itba.fieldops.domain.event;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.IncidentId;
import edu.itba.fieldops.domain.tracking.IncidentDescription;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record IncidentReported(
        ExpeditionId expeditionId,
        Instant occurredAt,
        IncidentId incidentId,
        Optional<ActivityId> activityId,
        IncidentDescription description
) implements DomainEvent {
    public IncidentReported {
        Objects.requireNonNull(expeditionId, "expedition id");
        Objects.requireNonNull(occurredAt, "occurred at");
        Objects.requireNonNull(incidentId, "incident id");
        Objects.requireNonNull(activityId, "affected activity");
        Objects.requireNonNull(description, "description");
    }
}
