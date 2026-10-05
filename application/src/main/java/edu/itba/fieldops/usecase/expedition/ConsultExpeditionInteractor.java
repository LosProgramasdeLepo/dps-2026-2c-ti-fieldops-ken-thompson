package edu.itba.fieldops.usecase.expedition;

import edu.itba.fieldops.domain.identity.ExpeditionId;

import java.util.Objects;

public final class ConsultExpeditionInteractor implements ConsultExpedition {
    private final ExpeditionRepository plans;

    public ConsultExpeditionInteractor(ExpeditionRepository plans) {
        this.plans = Objects.requireNonNull(plans, "plans");
    }

    @Override
    public PlanSnapshot of(ExpeditionId expeditionId) {
        return PlanSnapshot.of(plans.require(expeditionId));
    }
}
