package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.tracking.ActivityExecution;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.domain.tracking.InvalidActivityExecution;
import edu.itba.fieldops.domain.tracking.Observation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class ExpeditionExecution {
    public enum Status {
        IN_PROGRESS,
        SUSPENDED,
        FINISHED
    }

    private final ExpeditionId expeditionId;
    private Status status;
    private final List<ActivityExecution> activities = new ArrayList<>();
    private final List<Incident> incidents = new ArrayList<>();
    private final List<Observation> observations = new ArrayList<>();

    private ExpeditionExecution(ExpeditionId expeditionId, Status status) {
        this.expeditionId = Objects.requireNonNull(expeditionId, "expedition id");
        this.status = status;
    }

    public static ExpeditionExecution started(ExpeditionId expeditionId) {
        return new ExpeditionExecution(expeditionId, Status.IN_PROGRESS);
    }

    public void suspend() {
        requireStatus(Status.IN_PROGRESS, "suspend");
        status = Status.SUSPENDED;
    }

    public void resume() {
        requireStatus(Status.SUSPENDED, "resume");
        status = Status.IN_PROGRESS;
    }

    public void finish(Set<ActivityId> planned) {
        requireStatus(Status.IN_PROGRESS, "finish");
        requireStartedFinished();
        requireFinished(planned);
        status = Status.FINISHED;
    }

    public void startActivity(ActivityId activityId, Instant at, Set<ActivityId> predecessors) {
        requireStatus(Status.IN_PROGRESS, "start activity");
        Objects.requireNonNull(at, "started at");
        if (executionOf(activityId).isPresent()) {
            throw new InvalidActivityExecution("activity already started: " + activityId);
        }
        requirePredecessorsFinishedBy(predecessors, at);
        activities.add(new ActivityExecution(activityId, at));
    }

    public void finishActivity(ActivityId activityId, Instant at, String result) {
        requireStatus(Status.IN_PROGRESS, "finish activity");
        for (int index = 0; index < activities.size(); index++) {
            ActivityExecution execution = activities.get(index);
            if (execution.activityId().equals(activityId)) {
                activities.set(index, execution.finish(at, result));
                return;
            }
        }
        throw new InvalidActivityExecution("activity not started: " + activityId);
    }

    public void addIncident(Incident incident) {
        requireActive("record incident");
        incidents.add(Objects.requireNonNull(incident, "incident"));
    }

    public void addObservation(Observation observation) {
        requireActive("record observation");
        observations.add(Objects.requireNonNull(observation, "observation"));
    }

    public ExpeditionId expeditionId() {
        return expeditionId;
    }

    public Status status() {
        return status;
    }

    public boolean isFinished() {
        return status == Status.FINISHED;
    }

    public boolean hasStarted(ActivityId activityId) {
        return executionOf(Objects.requireNonNull(activityId, "activity id")).isPresent();
    }

    public List<ActivityExecution> activities() {
        return List.copyOf(activities);
    }

    public List<Incident> incidents() {
        return List.copyOf(incidents);
    }

    public List<Observation> observations() {
        return List.copyOf(observations);
    }

    private void requireStartedFinished() {
        for (ActivityExecution execution : activities) {
            if (!execution.isFinished()) {
                throw new InvalidActivityExecution("activity not finished: " + execution.activityId());
            }
        }
    }

    private void requireFinished(Set<ActivityId> planned) {
        for (ActivityId activityId : planned) {
            if (executionOf(activityId).filter(ActivityExecution::isFinished).isEmpty()) {
                throw new InvalidActivityExecution("activity not finished: " + activityId);
            }
        }
    }

    private void requirePredecessorsFinishedBy(Set<ActivityId> predecessors, Instant at) {
        for (ActivityId predecessorId : predecessors) {
            boolean ready = executionOf(predecessorId)
                    .flatMap(ActivityExecution::finishedAt)
                    .filter(end -> !at.isBefore(end))
                    .isPresent();
            if (!ready) {
                throw new InvalidActivityExecution("predecessor must finish before activity starts: " + predecessorId);
            }
        }
    }

    private Optional<ActivityExecution> executionOf(ActivityId activityId) {
        return activities.stream().filter(execution -> execution.activityId().equals(activityId)).findFirst();
    }

    private void requireStatus(Status expected, String action) {
        if (status != expected) {
            throw new InvalidExpeditionTransition(status, action);
        }
    }

    private void requireActive(String action) {
        if (status != Status.IN_PROGRESS && status != Status.SUSPENDED) {
            throw new InvalidExpeditionTransition(status, action);
        }
    }
}
