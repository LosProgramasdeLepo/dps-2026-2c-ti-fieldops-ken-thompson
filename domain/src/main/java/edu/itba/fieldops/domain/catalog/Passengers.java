package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.shared.InvalidValue;

public record Passengers(int count) {
    public Passengers {
        if (count < 0) {
            throw new InvalidValue("passengers must not be negative: " + count);
        }
    }
}
