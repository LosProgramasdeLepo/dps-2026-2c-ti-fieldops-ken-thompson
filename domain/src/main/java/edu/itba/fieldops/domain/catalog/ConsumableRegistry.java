package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.identity.ConsumableId;

public interface ConsumableRegistry extends Consumables {
    ConsumableId nextConsumableId();

    void save(Consumable consumable);
}
