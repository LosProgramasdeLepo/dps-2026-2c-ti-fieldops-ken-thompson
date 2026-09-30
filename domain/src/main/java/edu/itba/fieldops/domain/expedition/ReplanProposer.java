package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.BookableResources;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ProposalId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.shared.InvalidExpeditionTransition;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.tracking.ExpeditionExecution;
import edu.itba.fieldops.domain.tracking.Incident;

import java.time.Duration;
import java.util.Objects;

public final class ReplanProposer {
    private final Replanner replanner;

    public ReplanProposer(Replanner replanner) {
        this.replanner = Objects.requireNonNull(replanner, "replanner");
    }

    public ReplanProposal propose(
            Expedition plan,
            ExpeditionExecution execution,
            Incident incident,
            BookableResources resources,
            OccupyingExpeditions peers,
            ProposalId id
    ) {
        ActivityId activityId = incident.activityId();
        if (activityId == null) {
            throw new InvalidValue("incident must affect an activity");
        }
        if (plan.status() != ExpeditionStatus.APPROVED) {
            throw new InvalidExpeditionTransition(plan.status(), "propose replan");
        }
        Expedition suggested = plan.reviseAsDraft();
        Activity activity = suggested.activityOf(activityId);
        if (execution.hasStarted(activityId)) {
            replanner.cancel(suggested, activityId, resources, peers);
        } else if (incident.at().isAfter(activity.window().start())) {
            Duration delay = Duration.between(activity.window().start(), incident.at());
            if (suggested.delayFits(activityId, delay)) {
                replanner.delay(suggested, activityId, delay, resources, peers);
            } else {
                replanner.cancel(suggested, activityId, resources, peers);
            }
        } else {
            replanner.replaceUnavailable(suggested, resources, peers);
        }
        return new ReplanProposal(id, plan.id(), incident, suggested);
    }
}
