package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.tracking.Incident;

import java.time.Instant;
import java.util.Set;

public final class ExecutionEditing {
    private ExecutionEditing() {
    }

    public static ExpeditionExecution started(ExpeditionId expeditionId) {
        return ExpeditionExecution.started(expeditionId);
    }

    public static void startActivity(ExpeditionExecution execution, ActivityId activityId, Instant at, Set<ActivityId> predecessors) {
        execution.startActivity(activityId, at, predecessors);
    }

    public static void finishActivity(ExpeditionExecution execution, ActivityId activityId, Instant at, String result) {
        execution.finishActivity(activityId, at, result);
    }

    public static void finish(ExpeditionExecution execution, Set<ActivityId> planned) {
        execution.finish(planned);
    }

    public static void addIncident(ExpeditionExecution execution, Incident incident) {
        execution.addIncident(incident);
    }
}
