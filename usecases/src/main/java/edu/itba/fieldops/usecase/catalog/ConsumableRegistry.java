package edu.itba.fieldops.usecase.catalog;

import edu.itba.fieldops.domain.catalog.Consumable;
import edu.itba.fieldops.domain.catalog.Consumables;
import edu.itba.fieldops.domain.identity.ConsumableId;

public interface ConsumableRegistry extends Consumables {
    ConsumableId nextConsumableId();

    void save(Consumable consumable);
}
