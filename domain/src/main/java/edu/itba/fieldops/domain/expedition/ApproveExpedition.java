package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.assessment.ValidationResult;
import edu.itba.fieldops.domain.catalog.Catalog;
import edu.itba.fieldops.domain.shared.InvalidExpeditionTransition;
import edu.itba.fieldops.domain.validation.ExpeditionValidator;

import java.util.Objects;

public final class ApproveExpedition {
    private final ExpeditionValidator validator;

    public ApproveExpedition(ExpeditionValidator validator) {
        this.validator = Objects.requireNonNull(validator, "validator");
    }

    public void approve(Expedition plan, Catalog catalog, OccupyingExpeditions peers) {
        Objects.requireNonNull(plan, "expedition");
        Objects.requireNonNull(catalog, "catalog");
        Objects.requireNonNull(peers, "peers");
        if (plan.status() != ExpeditionStatus.IN_REVIEW) {
            throw new InvalidExpeditionTransition(plan.status(), "approve");
        }
        ValidationResult result = validator.validate(plan, catalog, peers);
        if (result.hasCritical()) {
            throw new ExpeditionNotApprovable("critical validation issues remain");
        }
        for (ValidationIssue warning : result.warnings()) {
            if (!plan.hasAccepted(warning)) {
                throw new ExpeditionNotApprovable("warning not justified: " + warning.code());
            }
        }
        plan.markApproved();
    }
}
