package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Catalogs;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.tracking.ExecutionRepository;
import edu.itba.fieldops.domain.tracking.ExpeditionExecution;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class PlanningContexts {
    private final ExpeditionRepository plans;
    private final ExecutionRepository executions;
    private final Catalogs catalogs;

    PlanningContexts(ExpeditionRepository plans, ExecutionRepository executions, Catalogs catalogs) {
        this.plans = Objects.requireNonNull(plans, "plans");
        this.executions = Objects.requireNonNull(executions, "executions");
        this.catalogs = Objects.requireNonNull(catalogs, "catalogs");
    }

    PlanningContext around(Expedition plan) {
        List<Expedition> all = plans.all();
        Map<ExpeditionId, ExpeditionExecution> runs = new HashMap<>();
        for (Expedition other : all) {
            executions.find(other.id()).ifPresent(run -> runs.put(other.id(), run));
        }
        return new PlanningContext(plan, catalogs, OccupyingExpeditions.of(plan, all, runs));
    }
}
