package edu.itba.fieldops.domain.event;

import edu.itba.fieldops.domain.shared.InvalidValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeliveryTest {
    @Test
    void aFailedDeliveryCanBeRetried() {
        assertDoesNotThrow(() -> delivery(DeliveryStatus.FAILED, 5).requireRetriable());
    }

    @ParameterizedTest
    @EnumSource(value = DeliveryStatus.class, names = "FAILED", mode = EnumSource.Mode.EXCLUDE)
    void anyOtherDeliveryCannotBeRetried(DeliveryStatus status) {
        Delivery delivery = delivery(status, 1);

        InvalidDeliveryTransition rejected = assertThrows(InvalidDeliveryTransition.class, delivery::requireRetriable);

        assertEquals("delivery to replan cannot be retried from " + status, rejected.getMessage());
    }

    @Test
    void attemptsCannotBeNegative() {
        assertThrows(InvalidValue.class, () -> delivery(DeliveryStatus.PENDING, -1));
    }

    @Test
    void rejectsNulls() {
        Optional<Instant> never = Optional.empty();
        Optional<String> noError = Optional.empty();

        assertThrows(NullPointerException.class, () -> new Delivery(null, DeliveryStatus.PENDING, 0, never, noError));
        assertThrows(NullPointerException.class, () -> new Delivery("replan", null, 0, never, noError));
        assertThrows(NullPointerException.class, () -> new Delivery("replan", DeliveryStatus.PENDING, 0, null, noError));
        assertThrows(NullPointerException.class, () -> new Delivery("replan", DeliveryStatus.PENDING, 0, never, null));
    }

    private static Delivery delivery(DeliveryStatus status, int attempts) {
        return new Delivery("replan", status, attempts, Optional.empty(), Optional.empty());
    }
}
