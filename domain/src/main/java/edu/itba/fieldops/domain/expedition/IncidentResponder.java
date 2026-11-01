package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.tracking.Incident;

import java.util.Objects;
import java.util.Optional;

public final class IncidentResponder {
    private final Plans plans;
    private final Replanner replanner;

    public IncidentResponder(Plans plans, Replanner replanner) {
        this.plans = Objects.requireNonNull(plans, "plans");
        this.replanner = Objects.requireNonNull(replanner, "replanner");
    }

    public Optional<ReplanProposal> respondTo(ExpeditionExecution run, Incident incident, ReplanIds ids) {
        if (!run.incidents().contains(Objects.requireNonNull(incident, "incident"))) {
            throw new InvalidValue("incident is not recorded on the run");
        }
        Expedition inForce = Revisions.inForce(plans.require(run.expeditionId()), plans.all());
        boolean affected = inForce.status() == ExpeditionStatus.APPROVED
                && incident.activityId().filter(inForce::includes).isPresent();
        if (!affected) {
            return Optional.empty();
        }
        Expedition revision = inForce.reviseAsDraft(ids.revision());
        return Optional.of(replanner.propose(ids.proposal(), revision, incident, run));
    }
}
