package edu.itba.fieldops.domain.itinerary;

import edu.itba.fieldops.domain.shared.InvalidValue;

import java.time.Duration;
import java.util.Objects;

public record Delay(Duration duration) {
    public Delay {
        Objects.requireNonNull(duration, "delay");
        if (duration.isNegative() || duration.isZero()) {
            throw new InvalidValue("delay must be positive: " + duration);
        }
    }
}
