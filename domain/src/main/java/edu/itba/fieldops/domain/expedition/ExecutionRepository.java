package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.IncidentId;

public interface ExecutionRepository extends Runs {
    IncidentId nextIncidentId();

    void save(ExpeditionExecution execution);
}
