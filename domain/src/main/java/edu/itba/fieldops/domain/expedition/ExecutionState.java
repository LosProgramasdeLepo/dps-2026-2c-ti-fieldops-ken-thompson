package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.tracking.ActivityExecution;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.domain.tracking.Observation;

import java.util.List;
import java.util.Objects;

public record ExecutionState(
        ExpeditionId expeditionId,
        ExpeditionExecution.Status status,
        List<ActivityExecution> activities,
        List<Incident> incidents,
        List<Observation> observations
) {
    public ExecutionState {
        Objects.requireNonNull(expeditionId, "expedition id");
        Objects.requireNonNull(status, "status");
        activities = List.copyOf(activities);
        incidents = List.copyOf(incidents);
        observations = List.copyOf(observations);
    }
}
