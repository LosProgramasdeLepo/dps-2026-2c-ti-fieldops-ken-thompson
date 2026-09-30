package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.assessment.ValidationResult;
import edu.itba.fieldops.domain.catalog.Catalogs;
import edu.itba.fieldops.domain.expedition.usecase.ApproveExpedition;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.shared.InvalidExpeditionTransition;
import edu.itba.fieldops.domain.tracking.ExecutionRepository;

import java.util.Objects;

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
        if (plan.status() != ExpeditionStatus.IN_REVIEW) {
            throw new InvalidExpeditionTransition(plan.status(), "approve");
        }
        requireApprovable(plan, validator.validate(contexts.around(plan)));
        plan.markApproved();
        plans.save(plan);
    }

    private static void requireApprovable(Expedition plan, ValidationResult result) {
        if (result.hasCritical()) {
            throw new ExpeditionNotApprovable("critical validation issues remain");
        }
        for (ValidationIssue warning : result.warnings()) {
            if (!plan.hasAccepted(warning)) {
                throw new ExpeditionNotApprovable("warning not justified: " + warning.code());
            }
        }
    }
}
