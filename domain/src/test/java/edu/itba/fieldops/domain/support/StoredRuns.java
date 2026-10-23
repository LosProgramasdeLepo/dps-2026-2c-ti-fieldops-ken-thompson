package edu.itba.fieldops.domain.support;

import edu.itba.fieldops.domain.expedition.ExpeditionExecution;
import edu.itba.fieldops.domain.expedition.Runs;
import edu.itba.fieldops.domain.identity.ExpeditionId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class StoredRuns implements Runs {
    private final Map<ExpeditionId, ExpeditionExecution> runs = new HashMap<>();

    public void save(ExpeditionExecution execution) {
        runs.put(execution.expeditionId(), execution);
    }

    @Override
    public Optional<ExpeditionExecution> find(ExpeditionId expeditionId) {
        return Optional.ofNullable(runs.get(expeditionId));
    }
}
