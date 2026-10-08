package edu.itba.fieldops.adapters.jpa.catalog;

import edu.itba.fieldops.domain.catalog.Consumable;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.usecase.catalog.ConsumableRegistry;
import edu.itba.fieldops.usecase.shared.Page;
import edu.itba.fieldops.usecase.shared.PageRequest;
import org.springframework.transaction.support.TransactionOperations;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class JpaConsumableRegistry implements ConsumableRegistry {
    private final StoredCatalog<ConsumableEntity, Consumable> stored;

    public JpaConsumableRegistry(ConsumableJpaRepository repository, TransactionOperations transactions) {
        this.stored = new StoredCatalog<>(repository, ConsumableEntity::toDomain, transactions);
    }

    @Override
    public ConsumableId nextConsumableId() {
        return new ConsumableId(UUID.randomUUID());
    }

    @Override
    public void save(Consumable consumable) {
        Objects.requireNonNull(consumable, "consumable");
        stored.save(() -> new ConsumableEntity(consumable));
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
