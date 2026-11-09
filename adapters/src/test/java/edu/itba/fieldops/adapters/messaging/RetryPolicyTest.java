package edu.itba.fieldops.adapters.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RetryPolicyTest {
    private static final Instant AT = Instant.parse("2026-11-01T08:00:00Z");
    private static final Duration SECOND = Duration.ofSeconds(1);
    private static final Duration MINUTE = Duration.ofMinutes(1);

    @ParameterizedTest
    @CsvSource({"1,1", "2,2", "3,4", "4,8", "5,16", "6,32", "7,60", "8,60", "64,60", "2147483647,60"})
    void theWaitDoublesUpToTheMaximum(int attempt, long seconds) {
        assertEquals(Duration.ofSeconds(seconds), new RetryPolicy(Integer.MAX_VALUE, SECOND, MINUTE).delayAfter(attempt));
    }

    @Test
    void aHugeMaximumDoesNotOverflow() {
        Duration huge = Duration.ofSeconds(Long.MAX_VALUE);

        assertEquals(huge, new RetryPolicy(Integer.MAX_VALUE, SECOND, huge).delayAfter(Integer.MAX_VALUE));
    }

    @Test
    void theNextAttemptComesAfterTheWait() {
        assertEquals(Optional.of(AT.plusSeconds(2)), new RetryPolicy(3, SECOND, MINUTE).nextAttemptAfter(2, AT));
    }

    @Test
    void theLastAttemptHasNoNext() {
        assertEquals(Optional.empty(), new RetryPolicy(3, SECOND, MINUTE).nextAttemptAfter(3, AT));
    }

    @Test
    void aSingleAttemptNeverRetries() {
        assertEquals(Optional.empty(), new RetryPolicy(1, SECOND, MINUTE).nextAttemptAfter(1, AT));
    }

    @Test
    void rejectsAnInvalidConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new RetryPolicy(0, SECOND, MINUTE));
        assertThrows(IllegalArgumentException.class, () -> new RetryPolicy(1, Duration.ZERO, MINUTE));
        assertThrows(IllegalArgumentException.class, () -> new RetryPolicy(1, MINUTE, SECOND));
    }
}
