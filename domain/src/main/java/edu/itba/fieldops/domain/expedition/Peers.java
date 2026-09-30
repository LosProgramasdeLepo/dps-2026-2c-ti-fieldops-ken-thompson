package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.tracking.ExecutionRepository;
import edu.itba.fieldops.domain.tracking.ExpeditionExecution;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class Peers {
    private Peers() {
    }

    static OccupyingExpeditions around(
            Expedition plan,
            ExpeditionRepository plans,
            ExecutionRepository executions
    ) {
        List<Expedition> all = plans.all();
        Map<ExpeditionId, ExpeditionExecution> runs = new HashMap<>();
        for (Expedition other : all) {
            executions.find(other.id()).ifPresent(run -> runs.put(other.id(), run));
        }
        return OccupyingExpeditions.of(plan, all, runs);
    }
}
