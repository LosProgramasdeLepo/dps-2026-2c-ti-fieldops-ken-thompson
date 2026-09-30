package edu.itba.fieldops.domain.shared;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StockTest {
    @ParameterizedTest
    @ValueSource(ints = {-1, -20})
    void rejectsANegativeAmount(int amount) {
        assertThrows(InvalidValue.class, () -> new Stock(amount));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 20})
    void acceptsAnEmptyOrPositiveAmount(int amount) {
        assertEquals(amount, new Stock(amount).amount());
    }
}
