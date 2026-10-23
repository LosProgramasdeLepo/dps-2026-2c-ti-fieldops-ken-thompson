package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Catalogs;
import edu.itba.fieldops.domain.identity.ExpeditionId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class PlanningContexts {
    private final Plans plans;
    private final Runs runs;
    private final Catalogs catalogs;

    public PlanningContexts(Plans plans, Runs runs, Catalogs catalogs) {
        this.plans = Objects.requireNonNull(plans, "plans");
        this.runs = Objects.requireNonNull(runs, "runs");
        this.catalogs = Objects.requireNonNull(catalogs, "catalogs");
    }

    public PlanningContext around(Expedition plan) {
        List<Expedition> all = plans.all();
        Map<ExpeditionId, ExpeditionExecution> executions = new HashMap<>();
        for (Expedition other : all) {
            runs.find(other.id()).ifPresent(run -> executions.put(other.id(), run));
        }
        return new PlanningContext(plan, catalogs, OccupyingExpeditions.of(plan, all, executions));
    }
}
