package edu.itba.fieldops.domain.shared;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PassengersTest {
    @ParameterizedTest
    @ValueSource(ints = {-1, -20})
    void rejectsANegativeCount(int count) {
        assertThrows(InvalidValue.class, () -> new Passengers(count));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 12})
    void acceptsAnEmptyOrPositiveCount(int count) {
        assertEquals(count, new Passengers(count).count());
    }
}
