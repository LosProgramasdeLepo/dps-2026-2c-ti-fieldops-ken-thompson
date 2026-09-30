package edu.itba.fieldops.domain.tracking;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.shared.InvalidExpeditionTransition;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.itinerary.Activity;

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
    private final List<ActivityExecution> executions = new ArrayList<>();
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

    public void finish(List<Activity> remaining) {
        requireStatus(Status.IN_PROGRESS, "finish");
        for (ActivityExecution execution : executions) {
            if (!execution.isFinished()) {
                throw new InvalidActivityExecution("activity not finished: " + execution.activityId());
            }
        }
        for (Activity activity : remaining) {
            if (executionOf(activity.id()).filter(ActivityExecution::isFinished).isEmpty()) {
                throw new InvalidActivityExecution("activity not finished: " + activity.id());
            }
        }
        status = Status.FINISHED;
    }

    public void startActivity(ActivityId activityId, Instant at, Set<ActivityId> predecessors) {
        requireStatus(Status.IN_PROGRESS, "start activity");
        Objects.requireNonNull(at, "started at");
        Objects.requireNonNull(predecessors, "predecessors");
        if (executionOf(activityId).isPresent()) {
            throw new InvalidActivityExecution("activity already started: " + activityId);
        }
        for (ActivityId predecessorId : predecessors) {
            boolean ready = executionOf(predecessorId)
                    .flatMap(ActivityExecution::finishedAt)
                    .filter(end -> !at.isBefore(end))
                    .isPresent();
            if (!ready) {
                throw new InvalidActivityExecution("predecessor must finish before activity starts: " + predecessorId);
            }
        }
        executions.add(new ActivityExecution(activityId, at));
    }

    public void finishActivity(ActivityId activityId, Instant at, String result) {
        requireStatus(Status.IN_PROGRESS, "finish activity");
        for (int index = 0; index < executions.size(); index++) {
            ActivityExecution execution = executions.get(index);
            if (execution.activityId().equals(activityId)) {
                executions.set(index, execution.finish(at, result));
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

    public List<ActivityExecution> executions() {
        return List.copyOf(executions);
    }

    public List<Incident> incidents() {
        return List.copyOf(incidents);
    }

    public List<Observation> observations() {
        return List.copyOf(observations);
    }

    private Optional<ActivityExecution> executionOf(ActivityId activityId) {
        return executions.stream().filter(execution -> execution.activityId().equals(activityId)).findFirst();
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
