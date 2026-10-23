package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.shared.UnknownResource;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public interface Plans {
    Optional<Expedition> find(ExpeditionId id);

    List<Expedition> all();

    default Expedition require(ExpeditionId id) {
        return find(Objects.requireNonNull(id, "expedition id"))
                .orElseThrow(() -> new UnknownResource("expedition", id.value().toString()));
    }
}
