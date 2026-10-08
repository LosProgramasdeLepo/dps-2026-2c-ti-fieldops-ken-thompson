package edu.itba.fieldops.adapters.jpa.expedition;

import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.identity.ExpeditionId;

import java.util.Objects;
import java.util.Optional;

final class StoredExpeditions {
    private final ExpeditionJpaRepository expeditions;

    StoredExpeditions(ExpeditionJpaRepository expeditions) {
        this.expeditions = Objects.requireNonNull(expeditions, "expeditions");
    }

    void save(Expedition expedition) {
        expeditions.save(new ExpeditionEntity(Objects.requireNonNull(expedition, "expedition").state()));
    }

    Optional<Expedition> find(ExpeditionId id) {
        return expeditions.findById(Objects.requireNonNull(id, "expedition id").value()).map(ExpeditionEntity::toDomain);
    }
}
