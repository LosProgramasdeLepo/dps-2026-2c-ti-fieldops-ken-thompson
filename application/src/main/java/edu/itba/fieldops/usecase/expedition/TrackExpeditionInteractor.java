package edu.itba.fieldops.usecase.expedition;
import edu.itba.fieldops.domain.expedition.ExecutionRepository;
import edu.itba.fieldops.domain.expedition.ExpeditionRepository;
import edu.itba.fieldops.domain.usecase.expedition.RunSnapshot;
import edu.itba.fieldops.domain.usecase.expedition.TrackExpedition;

import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionExecution;
import edu.itba.fieldops.domain.expedition.InvalidExpeditionTransition;
import edu.itba.fieldops.domain.expedition.Revisions;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.domain.shared.Clock;
import edu.itba.fieldops.domain.tracking.Observation;

import java.util.ArrayList;
import java.util.List;
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
        Expedition plan = plans.require(expeditionId);
        List<Expedition> archive = plans.all();
        executions.save(plan.start(clock.now(), archive, runsOf(archive)));
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
        execution.finish(inForce(plans.require(expeditionId)));
        executions.save(execution);
    }

    @Override
    public void startActivity(ExpeditionId expeditionId, ActivityId activityId) {
        ExpeditionExecution execution = requireRun(expeditionId, "start activity");
        Expedition plan = plans.require(expeditionId);
        execution.startActivity(plan.startOf(activityId, clock.now(), plans.all()));
        executions.save(execution);
    }

    @Override
    public void finishActivity(ExpeditionId expeditionId, ActivityId activityId, String result) {
        ExpeditionExecution execution = requireRun(expeditionId, "finish activity");
        execution.finishActivity(activityId, clock.now(), result);
        executions.save(execution);
    }

    @Override
    public void addObservation(ExpeditionId expeditionId, String text) {
        ExpeditionExecution execution = requireRun(expeditionId, "record observation");
        execution.addObservation(new Observation(text, clock.now()));
        executions.save(execution);
    }

    @Override
    public RunSnapshot run(ExpeditionId expeditionId) {
        Expedition plan = plans.require(expeditionId);
        return executions.find(expeditionId)
                .map(execution -> RunSnapshot.of(execution, inForce(plan).id()))
                .orElseThrow(() -> new UnknownResource("run", expeditionId.value().toString()));
    }

    private List<ExpeditionExecution> runsOf(List<Expedition> archive) {
        List<ExpeditionExecution> runs = new ArrayList<>();
        for (Expedition stored : archive) {
            executions.find(stored.id()).ifPresent(runs::add);
        }
        return runs;
    }

    private Expedition inForce(Expedition plan) {
        return Revisions.inForce(plan, plans.all());
    }

    private ExpeditionExecution requireRun(ExpeditionId expeditionId, String action) {
        return executions.find(Objects.requireNonNull(expeditionId, "expedition id"))
                .orElseThrow(() -> new InvalidExpeditionTransition(plans.require(expeditionId).status(), action));
    }

}
