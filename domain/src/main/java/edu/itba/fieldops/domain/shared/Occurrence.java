package edu.itba.fieldops.domain.shared;

import java.time.Instant;
import java.util.Objects;

public final class Occurrence {
    private final Instant instant;

    Occurrence(TimePeriod period, Instant instant) {
        Objects.requireNonNull(period, "period");
        Objects.requireNonNull(instant, "instant");
        if (!period.contains(instant)) {
            throw new InvalidValue("instant is outside the period");
        }
        this.instant = instant;
    }

    public Instant instant() {
        return instant;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Occurrence occurrence && instant.equals(occurrence.instant);
    }

    @Override
    public int hashCode() {
        return instant.hashCode();
    }
}
