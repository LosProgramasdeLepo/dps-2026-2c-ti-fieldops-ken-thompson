package edu.itba.fieldops.domain.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsumptionTest {
    @ParameterizedTest
    @ValueSource(ints = {0, -1, -20})
    void rejectsAConsumptionOfNothing(int amount) {
        assertThrows(InvalidValue.class, () -> new Consumption(amount));
    }

    @Test
    void addsTwoConsumptions() {
        assertEquals(new Consumption(7), new Consumption(3).plus(new Consumption(4)));
    }
}
