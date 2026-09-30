package edu.itba.fieldops.details;

import edu.itba.fieldops.domain.expedition.ExecutionRepository;
import edu.itba.fieldops.domain.expedition.ExpeditionExecution;
import edu.itba.fieldops.domain.identity.ExpeditionId;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class InMemoryExecutionRepository implements ExecutionRepository {
    private final Map<ExpeditionId, ExpeditionExecution> executions = new LinkedHashMap<>();

    @Override
    public void save(ExpeditionExecution execution) {
        Objects.requireNonNull(execution, "execution");
        executions.put(execution.expeditionId(), execution);
    }

    @Override
    public Optional<ExpeditionExecution> find(ExpeditionId expeditionId) {
        Objects.requireNonNull(expeditionId, "expedition id");
        return Optional.ofNullable(executions.get(expeditionId));
    }
}
