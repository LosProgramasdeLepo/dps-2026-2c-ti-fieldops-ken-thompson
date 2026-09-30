package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.expedition.usecase.ConsultExpedition;
import edu.itba.fieldops.domain.expedition.usecase.PlanSnapshot;
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
