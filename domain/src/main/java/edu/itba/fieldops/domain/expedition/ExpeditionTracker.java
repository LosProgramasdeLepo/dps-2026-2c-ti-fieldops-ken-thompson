package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.shared.Clock;
import edu.itba.fieldops.domain.tracking.Outcome;

import java.util.Objects;

public final class ExpeditionTracker {
    private final Plans plans;
    private final Runs runs;
    private final Clock clock;

    public ExpeditionTracker(Plans plans, Runs runs, Clock clock) {
        this.plans = Objects.requireNonNull(plans, "plans");
        this.runs = Objects.requireNonNull(runs, "runs");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public ExpeditionExecution start(ExpeditionId expeditionId) {
        Expedition plan = plans.require(expeditionId);
        for (ExpeditionId member : Revisions.lineage(plan, plans.all())) {
            runs.find(member).ifPresent(run -> {
                throw new InvalidExpeditionTransition(run.status(), "start");
            });
        }
        return plan.start(clock.now());
    }

    public void startActivity(ExpeditionExecution run, ActivityId activityId) {
        Expedition plan = plans.require(run.expeditionId());
        run.startActivity(plan.startOf(activityId, clock.now(), plans.all()));
    }

    public void finishActivity(ExpeditionExecution run, ActivityId activityId, Outcome result) {
        run.finishActivity(activityId, inForce(run).finishAt(clock.now()), result);
    }

    public void finish(ExpeditionExecution run) {
        run.finish(inForce(run));
    }

    private Expedition inForce(ExpeditionExecution run) {
        return Revisions.inForce(plans.require(run.expeditionId()), plans.all());
    }
}
