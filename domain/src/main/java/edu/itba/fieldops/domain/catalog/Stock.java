package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.shared.Consumption;
import edu.itba.fieldops.domain.shared.InvalidValue;

public record Stock(int amount) {
    public Stock {
        if (amount < 0) {
            throw new InvalidValue("stock must not be negative: " + amount);
        }
    }

    public boolean covers(Consumption needed) {
        return amount >= needed.amount();
    }
}
