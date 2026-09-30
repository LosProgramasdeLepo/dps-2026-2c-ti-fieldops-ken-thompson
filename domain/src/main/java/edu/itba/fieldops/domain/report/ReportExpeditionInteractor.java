package edu.itba.fieldops.domain.report;

import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionRepository;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.report.usecase.ReportExpedition;
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
        Expedition expedition = plans.require(expeditionId);
        return executions.find(expeditionId)
                .map(execution -> OperationalReport.of(expedition, execution))
                .orElseGet(() -> OperationalReport.of(expedition));
    }
}
