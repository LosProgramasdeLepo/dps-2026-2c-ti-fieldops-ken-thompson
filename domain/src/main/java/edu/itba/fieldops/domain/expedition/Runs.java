package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.ExpeditionId;

import java.util.Optional;

public interface Runs {
    Optional<ExpeditionExecution> find(ExpeditionId expeditionId);
}
