package edu.itba.fieldops.usecase.expedition;

import edu.itba.fieldops.domain.catalog.Consumables;
import edu.itba.fieldops.domain.expedition.ExecutionRepository;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionExecution;
import edu.itba.fieldops.domain.expedition.ExpeditionRepository;
import edu.itba.fieldops.domain.expedition.ExpeditionTracker;
import edu.itba.fieldops.domain.expedition.InvalidExpeditionTransition;
import edu.itba.fieldops.domain.expedition.Revisions;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.shared.Clock;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.domain.tracking.ActivityReport;
import edu.itba.fieldops.domain.usecase.expedition.RunSnapshot;
import edu.itba.fieldops.domain.usecase.expedition.TrackExpedition;

import java.util.Objects;

public final class TrackExpeditionInteractor implements TrackExpedition {
    private final ExpeditionRepository plans;
    private final ExecutionRepository executions;
    private final ExpeditionTracker tracker;

    public TrackExpeditionInteractor(
            ExpeditionRepository plans,
            ExecutionRepository executions,
            Consumables consumables,
            Clock clock
    ) {
        this.plans = Objects.requireNonNull(plans, "plans");
        this.executions = Objects.requireNonNull(executions, "executions");
        this.tracker = new ExpeditionTracker(plans, executions, consumables, clock);
    }

    @Override
    public void start(ExpeditionId expeditionId) {
        executions.save(tracker.start(expeditionId));
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
        tracker.finish(execution);
        executions.save(execution);
    }

    @Override
    public void startActivity(ExpeditionId expeditionId, ActivityId activityId) {
        ExpeditionExecution execution = requireRun(expeditionId, "start activity");
        tracker.startActivity(execution, activityId);
        executions.save(execution);
    }

    @Override
    public void finishActivity(ExpeditionId expeditionId, ActivityId activityId, ActivityReport report) {
        ExpeditionExecution execution = requireRun(expeditionId, "finish activity");
        tracker.finishActivity(execution, activityId, report);
        executions.save(execution);
    }

    @Override
    public void addObservation(ExpeditionId expeditionId, String text) {
        ExpeditionExecution execution = requireRun(expeditionId, "record observation");
        tracker.observe(execution, text);
        executions.save(execution);
    }

    @Override
    public RunSnapshot run(ExpeditionId expeditionId) {
        Expedition plan = plans.require(expeditionId);
        return executions.find(expeditionId)
                .map(execution -> RunSnapshot.of(execution, inForce(plan).id()))
                .orElseThrow(() -> new UnknownResource("run", expeditionId.value().toString()));
    }

    private Expedition inForce(Expedition plan) {
        return Revisions.inForce(plan, plans.all());
    }

    private ExpeditionExecution requireRun(ExpeditionId expeditionId, String action) {
        return executions.find(Objects.requireNonNull(expeditionId, "expedition id"))
                .orElseThrow(() -> new InvalidExpeditionTransition(plans.require(expeditionId).status(), action));
    }

}
