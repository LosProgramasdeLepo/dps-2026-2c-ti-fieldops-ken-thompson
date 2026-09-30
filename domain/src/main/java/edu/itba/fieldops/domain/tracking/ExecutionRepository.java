package edu.itba.fieldops.domain.tracking;

import edu.itba.fieldops.domain.identity.ExpeditionId;

import java.util.Optional;

public interface ExecutionRepository {
    void save(ExpeditionExecution execution);

    Optional<ExpeditionExecution> find(ExpeditionId expeditionId);
}
