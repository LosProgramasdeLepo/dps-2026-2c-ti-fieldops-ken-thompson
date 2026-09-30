package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Catalogs;
import edu.itba.fieldops.domain.expedition.usecase.RecordIncident;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.shared.Clock;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.domain.tracking.InvalidActivityExecution;

import java.util.Objects;

public final class RecordIncidentInteractor implements RecordIncident {
    private final ExpeditionRepository plans;
    private final ExecutionRepository executions;
    private final Clock clock;
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
        this.clock = Objects.requireNonNull(clock, "clock");
        this.contexts = new PlanningContexts(plans, executions, catalogs);
        this.replanner = Objects.requireNonNull(replanner, "replanner");
        this.proposals = Objects.requireNonNull(proposals, "proposals");
    }

    @Override
    public void record(ExpeditionId expeditionId, String description) {
        ExpeditionExecution execution = requireRun(expeditionId);
        execution.addIncident(Incident.of(description, clock.now()));
        executions.save(execution);
    }

    @Override
    public void record(ExpeditionId expeditionId, String description, ActivityId activityId) {
        Expedition plan = plans.require(expeditionId);
        requireKnownActivity(plan, activityId);
        ExpeditionExecution execution = requireRun(expeditionId);
        Incident incident = Incident.affecting(activityId, description, clock.now());
        execution.addIncident(incident);
        executions.save(execution);
        if (plan.status() == ExpeditionStatus.APPROVED && hasActivity(plan, activityId)) {
            propose(plan, incident, execution);
        }
    }

    private void propose(Expedition plan, Incident incident, ExpeditionExecution execution) {
        Expedition revision = plan.reviseAsDraft(plans.nextId());
        replanner.respondTo(contexts.around(revision), incident, execution);
        proposals.save(new ReplanProposal(proposals.nextId(), incident, revision));
    }

    private void requireKnownActivity(Expedition plan, ActivityId activityId) {
        if (!hasActivity(plan, activityId) && !hasActivity(Revisions.inForce(plan, plans.all()), activityId)) {
            throw new InvalidActivityExecution("unknown activity: " + activityId);
        }
    }

    private ExpeditionExecution requireRun(ExpeditionId expeditionId) {
        return executions.find(Objects.requireNonNull(expeditionId, "expedition id"))
                .orElseThrow(() -> new InvalidExpeditionTransition(plans.require(expeditionId).status(), "record incident"));
    }

    private static boolean hasActivity(Expedition plan, ActivityId activityId) {
        return plan.activities().stream().anyMatch(activity -> activity.id().equals(activityId));
    }
}
