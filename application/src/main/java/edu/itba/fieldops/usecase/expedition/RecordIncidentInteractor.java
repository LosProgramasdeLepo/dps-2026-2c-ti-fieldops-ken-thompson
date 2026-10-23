package edu.itba.fieldops.usecase.expedition;

import edu.itba.fieldops.domain.catalog.Catalogs;
import edu.itba.fieldops.domain.expedition.ExecutionRepository;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionExecution;
import edu.itba.fieldops.domain.expedition.ExpeditionRepository;
import edu.itba.fieldops.domain.expedition.ExpeditionTracker;
import edu.itba.fieldops.domain.expedition.InvalidExpeditionTransition;
import edu.itba.fieldops.domain.expedition.PlanningContexts;
import edu.itba.fieldops.domain.expedition.ReplanProposalRepository;
import edu.itba.fieldops.domain.expedition.Replanner;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.shared.Clock;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.domain.usecase.expedition.RecordIncident;

import java.util.Objects;

public final class RecordIncidentInteractor implements RecordIncident {
    private final ExpeditionRepository plans;
    private final ExecutionRepository executions;
    private final ExpeditionTracker tracker;
    private final PlanningContexts contexts;
    private final Replanner replanner;
    private final ReplanProposalRepository proposals;

    public RecordIncidentInteractor(
            ExpeditionRepository plans,
            ExecutionRepository executions,
            Clock clock,
            Catalogs catalogs,
            Replanner replanner,
            ReplanProposalRepository proposals
    ) {
        this.plans = Objects.requireNonNull(plans, "plans");
        this.executions = Objects.requireNonNull(executions, "executions");
        this.tracker = new ExpeditionTracker(plans, executions, catalogs.consumables(), clock);
        this.contexts = new PlanningContexts(plans, executions, catalogs);
        this.replanner = Objects.requireNonNull(replanner, "replanner");
        this.proposals = Objects.requireNonNull(proposals, "proposals");
    }

    @Override
    public void record(ExpeditionId expeditionId, String description) {
        ExpeditionExecution execution = requireRun(expeditionId);
        tracker.reportIncident(execution, description);
        executions.save(execution);
    }

    @Override
    public void record(ExpeditionId expeditionId, String description, ActivityId activityId) {
        ExpeditionExecution execution = requireRun(expeditionId);
        Incident incident = tracker.reportIncident(execution, activityId, description);
        executions.save(execution);
        tracker.planAffectedBy(execution, incident).ifPresent(plan -> propose(plan, incident, execution));
    }

    private void propose(Expedition plan, Incident incident, ExpeditionExecution execution) {
        Expedition revision = plan.reviseAsDraft(plans.nextId());
        proposals.save(replanner.propose(proposals.nextId(), contexts.around(revision), incident, execution));
    }

    private ExpeditionExecution requireRun(ExpeditionId expeditionId) {
        return executions.find(Objects.requireNonNull(expeditionId, "expedition id"))
                .orElseThrow(() -> new InvalidExpeditionTransition(plans.require(expeditionId).status(), "record incident"));
    }
}
