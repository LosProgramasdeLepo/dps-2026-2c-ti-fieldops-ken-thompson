package edu.itba.fieldops.adapters;

import edu.itba.fieldops.domain.expedition.ExecutionRepository;
import edu.itba.fieldops.domain.expedition.ExpeditionExecution;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Repository
@Profile(PersistenceProfiles.MEMORY)
public class InMemoryExecutionRepository implements ExecutionRepository {
    private final Map<ExpeditionId, ExpeditionExecution> executions = new LinkedHashMap<>();

    @Override
    public synchronized void save(ExpeditionExecution execution) {
        Objects.requireNonNull(execution, "execution");
        executions.put(execution.expeditionId(), execution);
    }

    @Override
    public synchronized Optional<ExpeditionExecution> find(ExpeditionId expeditionId) {
        Objects.requireNonNull(expeditionId, "expedition id");
        return Optional.ofNullable(executions.get(expeditionId));
    }
}
