package edu.itba.fieldops.domain.report;

import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionRepository;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.tracking.ExecutionRepository;

import java.util.Objects;

public final class ReportExpeditionInteractor implements ReportExpedition {
    private final ExpeditionRepository plans;
    private final ExecutionRepository executions;

    public ReportExpeditionInteractor(ExpeditionRepository plans, ExecutionRepository executions) {
        this.plans = Objects.requireNonNull(plans, "plans");
        this.executions = Objects.requireNonNull(executions, "executions");
    }

    @Override
    public OperationalReport of(ExpeditionId expeditionId) {
        Expedition expedition = plans.find(Objects.requireNonNull(expeditionId, "expedition id"))
                .orElseThrow(() -> new InvalidValue("unknown expedition: " + expeditionId));
        return OperationalReport.of(expedition, executions.find(expeditionId).orElse(null));
    }
}
