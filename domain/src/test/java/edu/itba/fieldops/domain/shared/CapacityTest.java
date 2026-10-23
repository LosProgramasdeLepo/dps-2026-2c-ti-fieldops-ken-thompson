package edu.itba.fieldops.domain.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CapacityTest {
    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsAVehicleWithoutSeats(int seats) {
        assertThrows(InvalidValue.class, () -> new Capacity(seats));
    }

    @Test
    void combinedSeatsCarryUpToTheirSum() {
        Capacity combined = new Capacity(2).plus(new Capacity(3));

        assertAll(
                () -> assertEquals(new Capacity(5), combined),
                () -> assertTrue(combined.carries(new Passengers(5))),
                () -> assertFalse(combined.carries(new Passengers(6)))
        );
    }
}
