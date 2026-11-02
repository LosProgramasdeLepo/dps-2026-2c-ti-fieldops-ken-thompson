package edu.itba.fieldops.adapters.mongo.expedition;

import edu.itba.fieldops.adapters.mongo.RegistrationSequence;
import edu.itba.fieldops.domain.expedition.Expedition;

import java.util.Objects;
import java.util.Optional;

final class StoredPlans {
    private final ExpeditionMongoRepository expeditions;
    private final RegistrationSequence sequence;

    StoredPlans(ExpeditionMongoRepository expeditions, RegistrationSequence sequence) {
        this.expeditions = Objects.requireNonNull(expeditions, "expeditions");
        this.sequence = Objects.requireNonNull(sequence, "sequence");
    }

    void register(Expedition expedition) {
        Long order = registrationOf(expedition).orElseGet(() -> sequence.next(ExpeditionDocument.COLLECTION));
        expeditions.save(ExpeditionDocument.of(expedition, order));
    }

    void keep(Expedition expedition) {
        expeditions.save(ExpeditionDocument.of(expedition, registrationOf(expedition).orElse(null)));
    }

    Optional<Expedition> find(String id) {
        return expeditions.findById(id).map(ExpeditionDocument::toDomain);
    }

    private Optional<Long> registrationOf(Expedition expedition) {
        return expeditions.findById(expedition.id().value().toString()).map(ExpeditionDocument::registrationOrder);
    }
}
