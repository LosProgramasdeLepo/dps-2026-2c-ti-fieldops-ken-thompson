package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.assessment.ValidationResult;
import edu.itba.fieldops.domain.catalog.Catalogs;
import edu.itba.fieldops.domain.expedition.usecase.ApproveExpedition;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.shared.InvalidExpeditionTransition;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.tracking.ExecutionRepository;
import edu.itba.fieldops.domain.validation.ExpeditionValidator;

import java.util.Objects;

public final class ApproveExpeditionInteractor implements ApproveExpedition {
    private final ExpeditionRepository plans;
    private final ExecutionRepository executions;
    private final Catalogs catalogs;
    private final ExpeditionValidator validator;

    public ApproveExpeditionInteractor(
            ExpeditionRepository plans,
            ExecutionRepository executions,
            Catalogs catalogs,
            ExpeditionValidator validator
    ) {
        this.plans = Objects.requireNonNull(plans, "plans");
        this.executions = Objects.requireNonNull(executions, "executions");
        this.catalogs = Objects.requireNonNull(catalogs, "catalogs");
        this.validator = Objects.requireNonNull(validator, "validator");
    }

    @Override
    public void approve(ExpeditionId expeditionId) {
        Expedition plan = plans.find(Objects.requireNonNull(expeditionId, "expedition id"))
                .orElseThrow(() -> new InvalidValue("unknown expedition: " + expeditionId));
        if (plan.status() != ExpeditionStatus.IN_REVIEW) {
            throw new InvalidExpeditionTransition(plan.status(), "approve");
        }
        ValidationResult result = validator.validate(plan, catalogs, Peers.around(plan, plans, executions));
        if (result.hasCritical()) {
            throw new ExpeditionNotApprovable("critical validation issues remain");
        }
        for (ValidationIssue warning : result.warnings()) {
            if (!plan.hasAccepted(warning)) {
                throw new ExpeditionNotApprovable("warning not justified: " + warning.code());
            }
        }
        plan.markApproved();
        plans.save(plan);
    }
}
