package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Catalogs;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.shared.Stock;

import java.util.Objects;
import java.util.Optional;

public record ConsumableAssignment(ActivityId activityId, ConsumableId consumableId, Stock quantity) implements Assignment {
    public ConsumableAssignment {
        Objects.requireNonNull(activityId, "activity id");
        Objects.requireNonNull(consumableId, "consumable id");
        Objects.requireNonNull(quantity, "quantity");
        if (quantity.amount() == 0) {
            throw new InvalidAssignment("assigned quantity must be positive");
        }
    }

    @Override
    public Optional<String> unknownIn(Catalogs catalogs) {
        return catalogs.consumables().consumable(consumableId).isEmpty()
                ? Optional.of("consumable " + consumableId)
                : Optional.empty();
    }

    @Override
    public void fileInto(Assignments assignments) {
        assignments.file(this);
    }

    @Override
    public void withdrawFrom(Assignments assignments) {
        assignments.withdraw(this);
    }
}
