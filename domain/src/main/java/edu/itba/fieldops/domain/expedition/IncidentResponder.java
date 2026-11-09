package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.IncidentId;
import edu.itba.fieldops.domain.shared.Clock;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.tracking.Incident;

import java.util.Objects;
import java.util.Optional;

public final class IncidentResponder {
    private final Plans plans;
    private final Proposals proposals;
    private final Replanner replanner;
    private final Clock clock;

    public IncidentResponder(Plans plans, Proposals proposals, Replanner replanner, Clock clock) {
        this.plans = Objects.requireNonNull(plans, "plans");
        this.proposals = Objects.requireNonNull(proposals, "proposals");
        this.replanner = Objects.requireNonNull(replanner, "replanner");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public Optional<ReplanProposal> respondTo(ExpeditionExecution run, IncidentId incidentId, ReplanIds ids) {
        Incident incident = recorded(run, incidentId);
        if (proposals.forIncident(incidentId).isPresent()) {
            return Optional.empty();
        }
        Expedition inForce = Revisions.inForce(plans.require(run.expeditionId()), plans.all());
        boolean affected = inForce.status() == ExpeditionStatus.APPROVED
                && incident.activityId().filter(inForce::includes).isPresent();
        if (!affected) {
            return Optional.empty();
        }
        Expedition revision = inForce.reviseAsDraft(ids.revision());
        return Optional.of(replanner.propose(new ReplanTrigger(ids.proposal(), incident, clock.now()), revision, run));
    }

    private static Incident recorded(ExpeditionExecution run, IncidentId incidentId) {
        return run.incidents().stream()
                .filter(incident -> incident.id().equals(incidentId))
                .findFirst()
                .orElseThrow(() -> new InvalidValue("incident is not recorded on the run"));
    }
}
