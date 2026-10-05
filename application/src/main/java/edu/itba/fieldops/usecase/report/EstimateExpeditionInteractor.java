package edu.itba.fieldops.usecase.report;

import edu.itba.fieldops.domain.report.Estimate;
import edu.itba.fieldops.usecase.expedition.ExpeditionRepository;
import edu.itba.fieldops.domain.identity.ExpeditionId;

import java.util.Objects;

public final class EstimateExpeditionInteractor implements EstimateExpedition {
    private final ExpeditionRepository plans;

    public EstimateExpeditionInteractor(ExpeditionRepository plans) {
        this.plans = Objects.requireNonNull(plans, "plans");
    }

    @Override
    public Estimate of(ExpeditionId expeditionId) {
        return Estimate.of(plans.require(expeditionId));
    }
}
