package edu.itba.fieldops.usecase.expedition;

import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionRepository;
import edu.itba.fieldops.domain.expedition.ExpeditionValidator;
import edu.itba.fieldops.domain.expedition.PlanningContexts;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.usecase.expedition.ApproveExpedition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
@Transactional
public class ApproveExpeditionInteractor implements ApproveExpedition {
    private final ExpeditionRepository plans;
    private final PlanningContexts contexts;
    private final ExpeditionValidator validator;

    public ApproveExpeditionInteractor(ExpeditionRepository plans, PlanningContexts contexts, ExpeditionValidator validator) {
        this.plans = Objects.requireNonNull(plans, "plans");
        this.contexts = Objects.requireNonNull(contexts, "planning contexts");
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
