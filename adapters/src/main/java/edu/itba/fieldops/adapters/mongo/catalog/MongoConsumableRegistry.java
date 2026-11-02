package edu.itba.fieldops.adapters.mongo.catalog;

import edu.itba.fieldops.adapters.PersistenceProfiles;
import edu.itba.fieldops.adapters.mongo.RegistrationSequence;
import edu.itba.fieldops.domain.catalog.Consumable;
import edu.itba.fieldops.domain.catalog.ConsumableRegistry;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
@Profile(PersistenceProfiles.MONGO)
@Transactional(readOnly = true)
public class MongoConsumableRegistry implements ConsumableRegistry {
    private final StoredCatalog<ConsumableDocument, Consumable> stored;

    MongoConsumableRegistry(ConsumableMongoRepository repository, RegistrationSequence sequence) {
        this.stored = new StoredCatalog<>(repository, sequence, ConsumableDocument.COLLECTION, ConsumableDocument::toDomain);
    }

    @Override
    public ConsumableId nextConsumableId() {
        return new ConsumableId(UUID.randomUUID());
    }

    @Override
    @Transactional
    public void save(Consumable consumable) {
        Objects.requireNonNull(consumable, "consumable");
        stored.save(consumable.id().value(), order -> ConsumableDocument.of(consumable, order));
    }

    @Override
    public Optional<Consumable> consumable(ConsumableId id) {
        return stored.find(Objects.requireNonNull(id, "consumable id").value());
    }

    @Override
    public Page<Consumable> consumables(PageRequest request) {
        return stored.page(request);
    }
}
