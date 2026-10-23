package edu.itba.fieldops.usecase.expedition;

import edu.itba.fieldops.domain.catalog.Catalogs;
import edu.itba.fieldops.domain.expedition.ExecutionRepository;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionRepository;
import edu.itba.fieldops.domain.expedition.ExpeditionValidator;
import edu.itba.fieldops.domain.expedition.PlanningContexts;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.usecase.expedition.ApproveExpedition;

import java.util.Objects;
import java.util.Optional;

public final class ApproveExpeditionInteractor implements ApproveExpedition {
    private final ExpeditionRepository plans;
    private final PlanningContexts contexts;
    private final ExpeditionValidator validator;

    public ApproveExpeditionInteractor(
            ExpeditionRepository plans,
            ExecutionRepository executions,
            Catalogs catalogs,
            ExpeditionValidator validator
    ) {
        this.plans = Objects.requireNonNull(plans, "plans");
        this.contexts = new PlanningContexts(plans, executions, catalogs);
        this.validator = Objects.requireNonNull(validator, "validator");
    }

    @Override
    public void approve(ExpeditionId expeditionId) {
        Expedition plan = plans.require(expeditionId);
        Optional<Expedition> previous = plan.supersedes().map(plans::require);
        plan.approve(validator, contexts, previous);
        previous.ifPresent(plans::save);
        plans.save(plan);
    }
}
