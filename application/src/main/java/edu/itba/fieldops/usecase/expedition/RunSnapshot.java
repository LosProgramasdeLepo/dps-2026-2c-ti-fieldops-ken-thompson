package edu.itba.fieldops.usecase.expedition;

import edu.itba.fieldops.domain.expedition.ExpeditionExecution;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.tracking.ActivityExecution;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.domain.tracking.Observation;

import java.util.List;
import java.util.Objects;

public record RunSnapshot(
        ExpeditionId expeditionId,
        ExpeditionId inForce,
        ExpeditionExecution.Status status,
        List<ActivityExecution> activities,
        List<Incident> incidents,
        List<Observation> observations
) {
    public RunSnapshot {
        Objects.requireNonNull(expeditionId, "expedition id");
        Objects.requireNonNull(inForce, "plan in force");
        Objects.requireNonNull(status, "status");
        activities = List.copyOf(activities);
        incidents = List.copyOf(incidents);
        observations = List.copyOf(observations);
    }

    public static RunSnapshot of(ExpeditionExecution execution, ExpeditionId inForce) {
        return new RunSnapshot(
                execution.expeditionId(),
                inForce,
                execution.status(),
                execution.activities(),
                execution.incidents(),
                execution.observations()
        );
    }
}
