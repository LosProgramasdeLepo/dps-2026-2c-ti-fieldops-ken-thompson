package edu.itba.fieldops.usecase.expedition;

import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.shared.InvalidValue;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public interface ExpeditionRepository {
    ExpeditionId nextId();

    ActivityId nextActivityId();

    void save(Expedition expedition);

    Optional<Expedition> find(ExpeditionId id);

    List<Expedition> all();

    default Expedition require(ExpeditionId id) {
        return find(Objects.requireNonNull(id, "expedition id"))
                .orElseThrow(() -> new InvalidValue("unknown expedition: " + id));
    }
}
