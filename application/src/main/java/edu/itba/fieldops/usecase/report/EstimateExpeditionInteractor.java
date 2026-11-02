package edu.itba.fieldops.usecase.report;

import edu.itba.fieldops.domain.expedition.ExpeditionRepository;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.report.Estimate;
import edu.itba.fieldops.domain.usecase.report.EstimateExpedition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class EstimateExpeditionInteractor implements EstimateExpedition {
    private final ExpeditionRepository plans;

    public EstimateExpeditionInteractor(ExpeditionRepository plans) {
        this.plans = Objects.requireNonNull(plans, "plans");
    }

    @Override
    public Estimate of(ExpeditionId expeditionId) {
        return Estimate.of(plans.require(expeditionId));
    }
}
