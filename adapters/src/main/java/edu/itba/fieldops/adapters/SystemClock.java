package edu.itba.fieldops.adapters;

import edu.itba.fieldops.domain.shared.Clock;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public final class SystemClock implements Clock {
    @Override
    public Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
