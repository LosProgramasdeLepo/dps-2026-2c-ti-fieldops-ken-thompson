package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.expedition.usecase.TrackExpedition;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.shared.Clock;
import edu.itba.fieldops.domain.shared.InvalidExpeditionTransition;
import edu.itba.fieldops.domain.tracking.ExecutionRepository;
import edu.itba.fieldops.domain.tracking.ExpeditionExecution;
import edu.itba.fieldops.domain.tracking.InvalidActivityExecution;
import edu.itba.fieldops.domain.tracking.Observation;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
        Expedition plan = plans.require(expeditionId);
        requireNoRunInLineage(plan);
        if (plan.status() != ExpeditionStatus.APPROVED) {
            throw new InvalidExpeditionTransition(plan.status(), "start");
        }
        if (!plan.charter().period().contains(clock.now())) {
            throw new InvalidActivityExecution("start is outside the expedition period");
        }
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
        ExpeditionExecution execution = requireRun(expeditionId, "finish");
        execution.finish(activityIds(inForce(plans.require(expeditionId))));
        executions.save(execution);
    }

    @Override
    public void startActivity(ExpeditionId expeditionId, ActivityId activityId) {
        ExpeditionExecution execution = requireRun(expeditionId, "start activity");
        Expedition current = inForce(plans.require(expeditionId));
        Instant at = clock.now();
        requireInsideWindow(current.activityOf(activityId), at);
        execution.startActivity(activityId, at, current.predecessorsOf(activityId));
        executions.save(execution);
    }

    @Override
    public void finishActivity(ExpeditionId expeditionId, ActivityId activityId, String result) {
        Expedition plan = plans.require(expeditionId);
        ExpeditionExecution execution = requireRun(expeditionId, "finish activity");
        Activity activity = activityOn(inForce(plan), activityId).orElseGet(() -> plan.activityOf(activityId));
        Instant at = clock.now();
        requireInsideWindow(activity, at);
        execution.finishActivity(activityId, at, result);
        executions.save(execution);
    }

    @Override
    public void addObservation(ExpeditionId expeditionId, String text) {
        ExpeditionExecution execution = requireRun(expeditionId, "record observation");
        execution.addObservation(new Observation(text, clock.now()));
        executions.save(execution);
    }

    private void requireNoRunInLineage(Expedition plan) {
        executions.find(plan.id()).ifPresent(run -> {
            throw new InvalidExpeditionTransition(run.status(), "start");
        });
        plan.supersedes().flatMap(executions::find).filter(run -> !run.isFinished()).ifPresent(run -> {
            throw new InvalidExpeditionTransition(run.status(), "start");
        });
    }

    private Expedition inForce(Expedition plan) {
        return Revisions.inForce(plan, plans.all());
    }

    private ExpeditionExecution requireRun(ExpeditionId expeditionId, String action) {
        return executions.find(Objects.requireNonNull(expeditionId, "expedition id"))
                .orElseThrow(() -> new InvalidExpeditionTransition(plans.require(expeditionId).status(), action));
    }

    private static Optional<Activity> activityOn(Expedition plan, ActivityId activityId) {
        return plan.activities().stream().filter(activity -> activity.id().equals(activityId)).findFirst();
    }

    private static Set<ActivityId> activityIds(Expedition plan) {
        return plan.activities().stream().map(Activity::id).collect(Collectors.toSet());
    }

    private static void requireInsideWindow(Activity activity, Instant at) {
        if (!activity.window().contains(at)) {
            throw new InvalidActivityExecution("activity instant is outside the planned window");
        }
    }
}
