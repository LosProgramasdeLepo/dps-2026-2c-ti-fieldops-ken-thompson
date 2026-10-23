package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import edu.itba.fieldops.domain.shared.UnknownResource;

import java.util.Objects;

public interface ConsumableRegistry extends Consumables {
    ConsumableId nextConsumableId();

    void save(Consumable consumable);

    Page<Consumable> consumables(PageRequest request);

    default Consumable require(ConsumableId id) {
        return consumable(Objects.requireNonNull(id, "consumable id"))
                .orElseThrow(() -> new UnknownResource("consumable", id.value().toString()));
    }
}
