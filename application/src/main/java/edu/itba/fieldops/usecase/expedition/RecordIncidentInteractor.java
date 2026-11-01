package edu.itba.fieldops.usecase.expedition;

import edu.itba.fieldops.domain.catalog.Consumables;
import edu.itba.fieldops.domain.expedition.ExecutionRepository;
import edu.itba.fieldops.domain.expedition.ExpeditionExecution;
import edu.itba.fieldops.domain.expedition.ExpeditionRepository;
import edu.itba.fieldops.domain.expedition.ExpeditionTracker;
import edu.itba.fieldops.domain.expedition.IncidentResponder;
import edu.itba.fieldops.domain.expedition.InvalidExpeditionTransition;
import edu.itba.fieldops.domain.expedition.ReplanIds;
import edu.itba.fieldops.domain.expedition.ReplanProposalRepository;
import edu.itba.fieldops.domain.expedition.Replanner;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.shared.Clock;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.domain.tracking.IncidentDescription;
import edu.itba.fieldops.domain.usecase.expedition.RecordIncident;

import java.util.Objects;

public final class RecordIncidentInteractor implements RecordIncident {
    private final ExpeditionRepository plans;
    private final ExecutionRepository executions;
    private final ExpeditionTracker tracker;
    private final IncidentResponder responder;
    private final ReplanProposalRepository proposals;

    public RecordIncidentInteractor(
            ExpeditionRepository plans,
            ExecutionRepository executions,
            Consumables consumables,
            Clock clock,
            Replanner replanner,
            ReplanProposalRepository proposals
    ) {
        this.plans = Objects.requireNonNull(plans, "plans");
        this.executions = Objects.requireNonNull(executions, "executions");
        this.tracker = new ExpeditionTracker(plans, executions, consumables, clock);
        this.responder = new IncidentResponder(plans, replanner);
        this.proposals = Objects.requireNonNull(proposals, "proposals");
    }

    @Override
    public void record(ExpeditionId expeditionId, IncidentDescription description) {
        ExpeditionExecution execution = requireRun(expeditionId);
        tracker.reportIncident(execution, description);
        executions.save(execution);
    }

    @Override
    public void record(ExpeditionId expeditionId, IncidentDescription description, ActivityId activityId) {
        ExpeditionExecution execution = requireRun(expeditionId);
        Incident incident = tracker.reportIncident(execution, activityId, description);
        executions.save(execution);
        responder.respondTo(execution, incident, new ReplanIds(proposals.nextId(), plans.nextId()))
                .ifPresent(proposals::save);
    }

    private ExpeditionExecution requireRun(ExpeditionId expeditionId) {
        return executions.find(Objects.requireNonNull(expeditionId, "expedition id"))
                .orElseThrow(() -> new InvalidExpeditionTransition(plans.require(expeditionId).status(), "record incident"));
    }
}
