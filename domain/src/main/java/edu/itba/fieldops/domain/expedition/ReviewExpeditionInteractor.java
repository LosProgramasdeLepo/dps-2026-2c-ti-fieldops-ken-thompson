package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.assessment.ValidationResult;
import edu.itba.fieldops.domain.catalog.Catalogs;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.tracking.ExecutionRepository;
import edu.itba.fieldops.domain.validation.ExpeditionValidator;

import java.util.Objects;

public final class ReviewExpeditionInteractor implements ReviewExpedition {
    private final ExpeditionRepository plans;
    private final ExecutionRepository executions;
    private final Catalogs catalogs;
    private final ExpeditionValidator validator;

    public ReviewExpeditionInteractor(
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
    public void submit(ExpeditionId expeditionId) {
        Expedition expedition = require(expeditionId);
        expedition.submitForReview();
        plans.save(expedition);
    }

    @Override
    public ValidationResult validate(ExpeditionId expeditionId) {
        Expedition expedition = require(expeditionId);
        return validator.validate(expedition, catalogs, Peers.around(expedition, plans, executions));
    }

    @Override
    public void acceptWarning(ExpeditionId expeditionId, AcceptedWarning warning) {
        Expedition expedition = require(expeditionId);
        expedition.acceptWarning(warning);
        plans.save(expedition);
    }

    @Override
    public void returnToDraft(ExpeditionId expeditionId) {
        Expedition expedition = require(expeditionId);
        expedition.returnToDraft();
        plans.save(expedition);
    }

    private Expedition require(ExpeditionId expeditionId) {
        return plans.find(Objects.requireNonNull(expeditionId, "expedition id"))
                .orElseThrow(() -> new InvalidValue("unknown expedition: " + expeditionId));
    }
}
