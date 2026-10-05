package edu.itba.fieldops.adapters;

import edu.itba.fieldops.usecase.shared.Clock;

import java.time.Instant;

public final class SystemClock implements Clock {
    @Override
    public Instant now() {
        return Instant.now();
    }
}
