package edu.itba.fieldops.domain.report;

import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionRepository;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.report.usecase.EstimateExpedition;
import edu.itba.fieldops.domain.shared.InvalidValue;

import java.util.Objects;

public final class EstimateExpeditionInteractor implements EstimateExpedition {
    private final ExpeditionRepository plans;

    public EstimateExpeditionInteractor(ExpeditionRepository plans) {
        this.plans = Objects.requireNonNull(plans, "plans");
    }

    @Override
    public Estimate of(ExpeditionId expeditionId) {
        Expedition expedition = plans.find(Objects.requireNonNull(expeditionId, "expedition id"))
                .orElseThrow(() -> new InvalidValue("unknown expedition: " + expeditionId));
        return Estimate.of(expedition);
    }
}
