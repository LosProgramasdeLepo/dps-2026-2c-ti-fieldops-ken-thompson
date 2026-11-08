package edu.itba.fieldops.usecase.expedition;

import edu.itba.fieldops.domain.event.EventLog;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionApprover;
import edu.itba.fieldops.domain.expedition.ExpeditionRepository;
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
    private final ExpeditionApprover approver;
    private final EventLog events;

    public ApproveExpeditionInteractor(ExpeditionRepository plans, ExpeditionApprover approver, EventLog events) {
        this.plans = Objects.requireNonNull(plans, "plans");
        this.approver = Objects.requireNonNull(approver, "approver");
        this.events = Objects.requireNonNull(events, "event log");
    }

    @Override
    public void approve(ExpeditionId expeditionId) {
        Expedition plan = plans.require(expeditionId);
        Optional<Expedition> previous = plan.supersedes().map(plans::require);
        approver.approve(plan, previous);
        previous.ifPresent(plans::save);
        plans.save(plan);
        events.append(plan.pullEvents());
    }
}
