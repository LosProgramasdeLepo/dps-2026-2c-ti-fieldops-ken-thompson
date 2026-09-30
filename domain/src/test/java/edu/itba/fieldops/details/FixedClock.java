package edu.itba.fieldops.details;

import edu.itba.fieldops.domain.shared.Clock;

import java.time.Instant;
import java.util.Objects;

public final class FixedClock implements Clock {
    private Instant now;

    public FixedClock(Instant now) {
        this.now = Objects.requireNonNull(now, "now");
    }

    @Override
    public Instant now() {
        return now;
    }

    public void set(Instant now) {
        this.now = Objects.requireNonNull(now, "now");
    }
}
