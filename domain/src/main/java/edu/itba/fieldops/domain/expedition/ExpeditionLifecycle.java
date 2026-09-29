package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.shared.InvalidExpeditionTransition;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.tracking.ExpeditionExecution;

import java.time.Instant;
import java.util.Objects;

public final class ExpeditionLifecycle {
    public ExpeditionExecution start(Expedition plan, ExpeditionExecution current) {
        Objects.requireNonNull(plan, "expedition");
        if (current != null) {
            throw new InvalidExpeditionTransition(current.status(), "start");
        }
        if (plan.status() != ExpeditionStatus.APPROVED) {
            throw new InvalidExpeditionTransition(plan.status(), "start");
        }
        return ExpeditionExecution.started(plan.id());
    }

    public ExpeditionExecution suspend(Expedition plan, ExpeditionExecution current) {
        Objects.requireNonNull(plan, "expedition");
        if (current == null) {
            if (plan.status() != ExpeditionStatus.APPROVED) {
                throw new InvalidExpeditionTransition(plan.status(), "suspend");
            }
            return ExpeditionExecution.suspended(plan.id());
        }
        requireSameExpedition(plan, current);
        current.suspend();
        return current;
    }

    public void finish(Expedition plan, ExpeditionExecution execution) {
        Objects.requireNonNull(plan, "expedition");
        requireSameExpedition(plan, execution);
        execution.finish(plan.itinerary());
    }

    public void returnToDraft(Expedition plan, ExpeditionExecution execution) {
        Objects.requireNonNull(plan, "expedition");
        if (execution != null) {
            requireSameExpedition(plan, execution);
        }
        if (execution != null && execution.isFinished()) {
            throw new InvalidExpeditionTransition(execution.status(), "return to draft");
        }
        plan.returnToDraft();
        if (execution != null) {
            execution.discard();
        }
    }

    public void startActivity(Expedition plan, ExpeditionExecution execution, ActivityId activityId, Instant at) {
        Objects.requireNonNull(plan, "expedition");
        requireSameExpedition(plan, execution);
        execution.startActivity(activityId, at, plan.activityOf(activityId).predecessors());
    }

    private static void requireSameExpedition(Expedition plan, ExpeditionExecution execution) {
        Objects.requireNonNull(execution, "execution");
        if (!execution.expeditionId().equals(plan.id())) {
            throw new InvalidValue("execution does not belong to the expedition");
        }
    }
}
