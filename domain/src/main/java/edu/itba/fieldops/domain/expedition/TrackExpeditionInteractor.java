package edu.itba.fieldops.domain.expedition;

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
        execution.finish(plan.itinerary());
        executions.save(execution);
    }

    @Override
    public void startActivity(ExpeditionId expeditionId, ActivityId activityId) {
        Expedition plan = requirePlan(expeditionId);
        ExpeditionExecution execution = requireRun(expeditionId, "start activity");
        Activity activity = plan.activityOf(activityId);
        Instant at = clock.now();
        requireInsideWindow(plan, activity, at);
        execution.startActivity(activityId, at, activity.predecessors());
        executions.save(execution);
    }

    @Override
    public void finishActivity(ExpeditionId expeditionId, ActivityId activityId, String result) {
        Expedition plan = requirePlan(expeditionId);
        ExpeditionExecution execution = requireRun(expeditionId, "finish activity");
        Instant at = clock.now();
        requireInsideWindow(plan, plan.activityOf(activityId), at);
        execution.finishActivity(activityId, at, result);
        executions.save(execution);
    }

    @Override
    public void addIncident(ExpeditionId expeditionId, Incident incident) {
        ExpeditionExecution execution = requireRun(expeditionId, "record incident");
        execution.addIncident(incident);
        executions.save(execution);
    }

    @Override
    public void addObservation(ExpeditionId expeditionId, Observation observation) {
        ExpeditionExecution execution = requireRun(expeditionId, "record observation");
        execution.addObservation(observation);
        executions.save(execution);
    }

    @Override
    public void returnToDraft(ExpeditionId expeditionId) {
        Expedition plan = requirePlan(expeditionId);
        ExpeditionExecution execution = executions.find(expeditionId).orElse(null);
        if (execution != null && !execution.isFinished()) {
            throw new InvalidExpeditionTransition(execution.status(), "return to draft");
        }
        plan.returnToDraft();
        plans.save(plan);
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
