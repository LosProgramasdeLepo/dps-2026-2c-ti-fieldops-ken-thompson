package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.ExpeditionId;

import java.util.List;
import java.util.Optional;

public interface ExpeditionRepository {
    ExpeditionId nextId();

    void save(Expedition expedition);

    Optional<Expedition> find(ExpeditionId id);

    List<Expedition> all();
}
