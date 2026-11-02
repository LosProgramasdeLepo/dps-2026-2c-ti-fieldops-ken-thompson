package edu.itba.fieldops.usecase.expedition;

import edu.itba.fieldops.domain.assessment.ValidationResult;
import edu.itba.fieldops.domain.expedition.AcceptedWarning;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionRepository;
import edu.itba.fieldops.domain.expedition.ExpeditionValidator;
import edu.itba.fieldops.domain.expedition.PlanningContexts;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.usecase.expedition.ReviewExpedition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional
public class ReviewExpeditionInteractor implements ReviewExpedition {
    private final ExpeditionRepository plans;
    private final PlanningContexts contexts;
    private final ExpeditionValidator validator;

    public ReviewExpeditionInteractor(ExpeditionRepository plans, PlanningContexts contexts, ExpeditionValidator validator) {
        this.plans = Objects.requireNonNull(plans, "plans");
        this.contexts = Objects.requireNonNull(contexts, "planning contexts");
        this.validator = Objects.requireNonNull(validator, "validator");
    }

    @Override
    public void submit(ExpeditionId expeditionId) {
        Expedition expedition = plans.require(expeditionId);
        expedition.submitForReview(validator, contexts);
        plans.save(expedition);
    }

    @Override
    @Transactional(readOnly = true)
    public ValidationResult validate(ExpeditionId expeditionId) {
        return validator.validate(contexts.around(plans.require(expeditionId)));
    }

    @Override
    public void acceptWarning(ExpeditionId expeditionId, AcceptedWarning warning) {
        Expedition expedition = plans.require(expeditionId);
        expedition.acceptWarning(validator, contexts, warning);
        plans.save(expedition);
    }

    @Override
    public void returnToDraft(ExpeditionId expeditionId) {
        Expedition expedition = plans.require(expeditionId);
        expedition.returnToDraft();
        plans.save(expedition);
    }
}
