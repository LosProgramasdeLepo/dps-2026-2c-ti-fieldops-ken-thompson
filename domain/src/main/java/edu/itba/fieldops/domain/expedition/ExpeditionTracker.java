package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Consumables;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.shared.Clock;
import edu.itba.fieldops.domain.shared.Occurrence;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.domain.tracking.ActivityReport;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.domain.tracking.InvalidActivityExecution;
import edu.itba.fieldops.domain.tracking.Observation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class ExpeditionTracker {
    private final Plans plans;
    private final Runs runs;
    private final Consumables consumables;
    private final Clock clock;

    public ExpeditionTracker(Plans plans, Runs runs, Consumables consumables, Clock clock) {
        this.plans = Objects.requireNonNull(plans, "plans");
        this.runs = Objects.requireNonNull(runs, "runs");
        this.consumables = Objects.requireNonNull(consumables, "consumables");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public ExpeditionExecution start(ExpeditionId expeditionId) {
        Expedition plan = plans.require(expeditionId);
        for (ExpeditionId member : Revisions.lineage(plan, plans.all())) {
            runs.find(member).ifPresent(run -> {
                throw new InvalidExpeditionTransition(run.status(), "start");
            });
        }
        return plan.start(clock.now());
    }

    public void startActivity(ExpeditionExecution run, ActivityId activityId) {
        Expedition plan = plans.require(run.expeditionId());
        run.startActivity(plan.startOf(activityId, clock.now(), plans.all()));
    }

    public void finishActivity(ExpeditionExecution run, ActivityId activityId, ActivityReport report) {
        for (ConsumableId consumableId : report.consumed().keySet()) {
            if (consumables.consumable(consumableId).isEmpty()) {
                throw new UnknownResource("consumable", consumableId.value().toString());
            }
        }
        run.finishActivity(activityId, now(run, "finish"), report);
    }

    public void finish(ExpeditionExecution run) {
        run.finish(inForce(run));
    }

    public Incident reportIncident(ExpeditionExecution run, String description) {
        Incident incident = Incident.of(description, now(run, "incident").instant());
        run.addIncident(incident);
        return incident;
    }

    public Incident reportIncident(ExpeditionExecution run, ActivityId activityId, String description) {
        requireInLineage(run, activityId);
        Incident incident = Incident.affecting(activityId, description, now(run, "incident").instant());
        run.addIncident(incident);
        return incident;
    }

    public void observe(ExpeditionExecution run, String text) {
        run.addObservation(new Observation(text, now(run, "observation").instant()));
    }

    public Optional<Expedition> planAffectedBy(ExpeditionExecution run, Incident incident) {
        Expedition inForce = inForce(run);
        return incident.activityId()
                .filter(activityId -> inForce.status() == ExpeditionStatus.APPROVED && inForce.includes(activityId))
                .map(activityId -> inForce);
    }

    private void requireInLineage(ExpeditionExecution run, ActivityId activityId) {
        List<Expedition> all = plans.all();
        Set<ExpeditionId> lineage = Revisions.lineage(inForce(run), all);
        boolean known = all.stream()
                .filter(plan -> lineage.contains(plan.id()))
                .anyMatch(plan -> plan.includes(activityId));
        if (!known) {
            throw new InvalidActivityExecution("unknown activity: " + activityId);
        }
    }

    private Occurrence now(ExpeditionExecution run, String event) {
        return inForce(run).eventAt(clock.now(), event);
    }

    private Expedition inForce(ExpeditionExecution run) {
        return Revisions.inForce(plans.require(run.expeditionId()), plans.all());
    }
}
