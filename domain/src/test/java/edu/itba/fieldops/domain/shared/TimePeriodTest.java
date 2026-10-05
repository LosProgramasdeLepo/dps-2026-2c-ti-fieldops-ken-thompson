package edu.itba.fieldops.domain.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimePeriodTest {
    private static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");

    @ParameterizedTest
    @CsvSource({
            "0, 4, 4, 8, false",
            "0, 2, 5, 6, false",
            "0, 4, 3, 6, true",
            "0, 8, 2, 4, true",
            "0, 4, 0, 4, true"
    })
    void overlapsOnlyWhenBothPeriodsShareTime(int firstFrom, int firstTo, int secondFrom, int secondTo, boolean overlaps) {
        TimePeriod first = hours(firstFrom, firstTo);
        TimePeriod second = hours(secondFrom, secondTo);

        assertEquals(overlaps, first.overlaps(second));
        assertEquals(overlaps, second.overlaps(first));
    }

    @ParameterizedTest
    @CsvSource({
            "0, 4, 4, 8, true",
            "0, 2, 5, 6, true",
            "0, 4, 3, 6, false",
            "4, 8, 0, 4, false"
    })
    void finishesBeforeAnotherStartsWhenItEndsNoLaterThanThatStart(int firstFrom, int firstTo, int secondFrom, int secondTo, boolean before) {
        assertEquals(before, hours(firstFrom, firstTo).finishesBeforeStartOf(hours(secondFrom, secondTo)));
    }

    @Test
    void containsClosedInterval() {
        TimePeriod week = hours(0, 24);
        TimePeriod morning = hours(0, 4);

        assertTrue(week.contains(morning));
        assertTrue(week.contains(week));
        assertFalse(morning.contains(week));
    }

    @Test
    void shiftedKeepsLength() {
        TimePeriod morning = hours(0, 4);

        TimePeriod delayed = morning.shifted(Duration.ofHours(2));

        assertEquals(hours(2, 6), delayed);
    }

    @Test
    void rejectsEndBeforeStart() {
        assertThrows(InvalidValue.class, () -> new TimePeriod(hours(4, 8).end(), hours(0, 4).start()));
    }

    private static TimePeriod hours(int from, int to) {
        return new TimePeriod(DAY.plusSeconds(from * 3600L), DAY.plusSeconds(to * 3600L));
    }
}
