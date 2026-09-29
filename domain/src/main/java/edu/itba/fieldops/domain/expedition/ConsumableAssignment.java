package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Catalog;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.shared.Stock;
import edu.itba.fieldops.domain.shared.TimePeriod;

import java.util.Objects;
import java.util.Optional;

public record ConsumableAssignment(ActivityId activityId, ConsumableId consumableId, Stock quantity) implements Assignment {
    public ConsumableAssignment {
        Objects.requireNonNull(activityId, "activity id");
        Objects.requireNonNull(consumableId, "consumable id");
        Objects.requireNonNull(quantity, "quantity");
    }

    @Override
    public Optional<TemporalBooking> booking(TimePeriod window) {
        return Optional.empty();
    }

    @Override
    public Optional<String> unknownIn(Catalog catalog) {
        return catalog.consumable(consumableId).isEmpty() ? Optional.of("consumable " + consumableId) : Optional.empty();
    }
}
