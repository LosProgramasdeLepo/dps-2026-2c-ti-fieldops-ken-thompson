package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.identity.ConsumableId;

import java.util.Optional;

public interface Consumables {
    Optional<Consumable> consumable(ConsumableId id);
}
