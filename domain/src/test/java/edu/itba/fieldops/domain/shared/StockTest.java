package edu.itba.fieldops.domain.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void coversAConsumptionUpToItsAmount() {
        Stock stock = new Stock(5);

        assertAll(
                () -> assertTrue(stock.covers(new Consumption(4))),
                () -> assertTrue(stock.covers(new Consumption(5))),
                () -> assertFalse(stock.covers(new Consumption(6)))
        );
    }
}
