package edu.itba.fieldops.adapters;

import edu.itba.fieldops.domain.catalog.Consumable;
import edu.itba.fieldops.domain.catalog.ConsumableRegistry;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryConsumableRegistry implements ConsumableRegistry {
    private final Map<ConsumableId, Consumable> consumables = new LinkedHashMap<>();

    @Override
    public ConsumableId nextConsumableId() {
        return new ConsumableId(UUID.randomUUID());
    }

    @Override
    public void save(Consumable consumable) {
        Objects.requireNonNull(consumable, "consumable");
        consumables.put(consumable.id(), consumable);
    }

    @Override
    public Optional<Consumable> consumable(ConsumableId id) {
        Objects.requireNonNull(id, "consumable id");
        return Optional.ofNullable(consumables.get(id));
    }

    @Override
    public Page<Consumable> consumables(PageRequest request) {
        return InMemoryPages.slice(consumables.values(), request);
    }
}
