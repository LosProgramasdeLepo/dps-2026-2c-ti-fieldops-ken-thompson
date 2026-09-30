package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.expedition.usecase.TrackExpedition;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.shared.Clock;
import edu.itba.fieldops.domain.shared.InvalidExpeditionTransition;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.tracking.ExecutionRepository;
import edu.itba.fieldops.domain.tracking.ExpeditionExecution;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.domain.tracking.InvalidActivityExecution;
import edu.itba.fieldops.domain.tracking.Observation;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class TrackExpeditionInteractor implements TrackExpedition {
    private final ExpeditionRepository plans;
    private final ExecutionRepository executions;
    private final Clock clock;

    public TrackExpeditionInteractor(ExpeditionRepository plans, ExecutionRepository executions, Clock clock) {
        this.plans = Objects.requireNonNull(plans, "plans");
        this.executions = Objects.requireNonNull(executions, "executions");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void start(ExpeditionId expeditionId) {
        Expedition plan = requirePlan(expeditionId);
        executions.find(expeditionId).ifPresent(run -> {
            throw new InvalidExpeditionTransition(run.status(), "start");
        });
        if (plan.status() != ExpeditionStatus.APPROVED) {
            throw new InvalidExpeditionTransition(plan.status(), "start");
        }
        plan.supersedes().flatMap(executions::find).filter(run -> !run.isFinished()).ifPresent(run -> {
            throw new InvalidExpeditionTransition(run.status(), "start");
        });
        requireInsidePeriod(plan, clock.now());
        executions.save(ExpeditionExecution.started(plan.id()));
    }

    @Override
    public void suspend(ExpeditionId expeditionId) {
        ExpeditionExecution execution = requireRun(expeditionId, "suspend");
        execution.suspend();
        executions.save(execution);
    }

    @Override
    public void resume(ExpeditionId expeditionId) {
        ExpeditionExecution execution = requireRun(expeditionId, "resume");
        execution.resume();
        executions.save(execution);
    }

    @Override
    public void finish(ExpeditionId expeditionId) {
        Expedition plan = requirePlan(expeditionId);
        ExpeditionExecution execution = requireRun(expeditionId, "finish");
        execution.finish(scheduleOf(plan).itinerary());
        executions.save(execution);
    }

    @Override
    public void startActivity(ExpeditionId expeditionId, ActivityId activityId) {
        Expedition plan = requirePlan(expeditionId);
        ExpeditionExecution execution = requireRun(expeditionId, "start activity");
        Activity activity = scheduleOf(plan).activityOf(activityId);
        Instant at = clock.now();
        requireInsideWindow(plan, activity, at);
        execution.startActivity(activityId, at, activity.predecessors());
        executions.save(execution);
    }

    @Override
    public void finishActivity(ExpeditionId expeditionId, ActivityId activityId, String result) {
        Expedition plan = requirePlan(expeditionId);
        ExpeditionExecution execution = requireRun(expeditionId, "finish activity");
        Activity activity = activityOn(scheduleOf(plan), activityId).orElseGet(() -> plan.activityOf(activityId));
        Instant at = clock.now();
        requireInsideWindow(plan, activity, at);
        execution.finishActivity(activityId, at, result);
        executions.save(execution);
    }

    @Override
    public void addIncident(ExpeditionId expeditionId, String description) {
        recordIncident(expeditionId, description, null);
    }

    @Override
    public void addIncident(ExpeditionId expeditionId, String description, ActivityId activityId) {
        Expedition plan = requirePlan(expeditionId);
        if (activityOn(plan, activityId).isEmpty() && activityOn(scheduleOf(plan), activityId).isEmpty()) {
            throw new InvalidActivityExecution("unknown activity: " + activityId);
        }
        recordIncident(expeditionId, description, activityId);
    }

    @Override
    public void addObservation(ExpeditionId expeditionId, String text) {
        ExpeditionExecution execution = requireRun(expeditionId, "record observation");
        execution.addObservation(new Observation(text, clock.now()));
        executions.save(execution);
    }

    private void recordIncident(ExpeditionId expeditionId, String description, ActivityId activityId) {
        ExpeditionExecution execution = requireRun(expeditionId, "record incident");
        execution.addIncident(new Incident(description, clock.now(), activityId));
        executions.save(execution);
    }

    private Expedition scheduleOf(Expedition plan) {
        Expedition current = plan;
        while (true) {
            Expedition next = null;
            for (Expedition candidate : plans.all()) {
                if (candidate.supersedes().filter(current.id()::equals).isPresent()) {
                    next = candidate;
                    break;
                }
            }
            if (next == null) {
                return current;
            }
            current = next;
        }
    }

    private static Optional<Activity> activityOn(Expedition expedition, ActivityId activityId) {
        return expedition.itinerary().stream().filter(activity -> activity.id().equals(activityId)).findFirst();
    }

    private static void requireInsidePeriod(Expedition plan, Instant at) {
        if (!plan.period().contains(at)) {
            throw new InvalidActivityExecution("start is outside the expedition period");
        }
    }

    private static void requireInsideWindow(Expedition plan, Activity activity, Instant at) {
        if (!plan.period().contains(at) || !activity.window().contains(at)) {
            throw new InvalidActivityExecution("activity instant is outside the planned window");
        }
    }

    private Expedition requirePlan(ExpeditionId expeditionId) {
        return plans.find(Objects.requireNonNull(expeditionId, "expedition id"))
                .orElseThrow(() -> new InvalidValue("unknown expedition: " + expeditionId));
    }

    private ExpeditionExecution requireRun(ExpeditionId expeditionId, String action) {
        return executions.find(Objects.requireNonNull(expeditionId, "expedition id"))
                .orElseThrow(() -> new InvalidExpeditionTransition(requirePlan(expeditionId).status(), action));
    }
}
