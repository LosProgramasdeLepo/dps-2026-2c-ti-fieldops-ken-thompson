package edu.itba.fieldops.domain.itinerary;

import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.TimePeriod;

import java.time.Duration;
import java.util.Objects;

public record EstimatedDuration(Duration value) {
    public EstimatedDuration {
        Objects.requireNonNull(value, "estimated duration");
        if (value.isNegative() || value.isZero()) {
            throw new InvalidValue("estimated duration must be positive: " + value);
        }
    }

    public boolean fitsIn(TimePeriod window) {
        return window.length().compareTo(value) >= 0;
    }
}
