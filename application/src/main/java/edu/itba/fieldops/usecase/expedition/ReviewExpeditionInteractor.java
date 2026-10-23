package edu.itba.fieldops.usecase.expedition;
import edu.itba.fieldops.domain.expedition.ExecutionRepository;
import edu.itba.fieldops.domain.expedition.ExpeditionRepository;
import edu.itba.fieldops.domain.usecase.expedition.ReviewExpedition;

import edu.itba.fieldops.domain.expedition.AcceptedWarning;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionNotApprovable;
import edu.itba.fieldops.domain.expedition.ExpeditionValidator;
import edu.itba.fieldops.domain.assessment.ValidationResult;
import edu.itba.fieldops.domain.catalog.Catalogs;
import edu.itba.fieldops.domain.identity.ExpeditionId;

import java.util.Objects;

public final class ReviewExpeditionInteractor implements ReviewExpedition {
    private final ExpeditionRepository plans;
    private final PlanningContexts contexts;
    private final ExpeditionValidator validator;

    public ReviewExpeditionInteractor(
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
    public void submit(ExpeditionId expeditionId) {
        Expedition expedition = plans.require(expeditionId);
        if (validationOf(expedition).hasCritical()) {
            throw new ExpeditionNotApprovable("critical validation issues remain");
        }
        expedition.submitForReview();
        plans.save(expedition);
    }

    @Override
    public ValidationResult validate(ExpeditionId expeditionId) {
        return validationOf(plans.require(expeditionId));
    }

    @Override
    public void acceptWarning(ExpeditionId expeditionId, AcceptedWarning warning) {
        Expedition expedition = plans.require(expeditionId);
        expedition.acceptWarning(validator, contexts.around(expedition), warning);
        plans.save(expedition);
    }

    @Override
    public void returnToDraft(ExpeditionId expeditionId) {
        Expedition expedition = plans.require(expeditionId);
        expedition.returnToDraft();
        plans.save(expedition);
    }

    private ValidationResult validationOf(Expedition expedition) {
        return validator.validate(contexts.around(expedition));
    }
}
