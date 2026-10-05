package edu.itba.fieldops.usecase.expedition;

import edu.itba.fieldops.domain.expedition.ExpeditionExecution;
import edu.itba.fieldops.domain.identity.ExpeditionId;

import java.util.Optional;

public interface ExecutionRepository {
    void save(ExpeditionExecution execution);

    Optional<ExpeditionExecution> find(ExpeditionId expeditionId);
}
