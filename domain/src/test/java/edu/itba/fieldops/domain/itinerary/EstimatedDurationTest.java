package edu.itba.fieldops.domain.itinerary;

import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.TimePeriod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstimatedDurationTest {
    private static final Instant START = Instant.parse("2026-11-01T08:00:00Z");

    @ParameterizedTest
    @ValueSource(strings = {"PT0S", "-PT1H"})
    void rejectsADurationThatTakesNoTime(String duration) {
        assertThrows(InvalidValue.class, () -> new EstimatedDuration(Duration.parse(duration)));
    }

    @Test
    void fitsAWindowAtLeastAsLong() {
        EstimatedDuration fourHours = new EstimatedDuration(Duration.ofHours(4));

        assertAll(
                () -> assertTrue(fourHours.fitsIn(new TimePeriod(START, START.plus(Duration.ofHours(4))))),
                () -> assertFalse(fourHours.fitsIn(new TimePeriod(START, START.plus(Duration.ofHours(3)))))
        );
    }
}
