package edu.itba.fieldops.adapters;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SystemClockTest {
    @Test
    void readsTheTimeWithTheMicrosecondsTheDatabaseKeeps() {
        Instant now = new SystemClock().now();

        assertEquals(now.truncatedTo(ChronoUnit.MICROS), now);
    }
}
