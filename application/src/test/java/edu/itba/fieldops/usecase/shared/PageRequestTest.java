package edu.itba.fieldops.usecase.shared;

import edu.itba.fieldops.domain.shared.InvalidValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PageRequestTest {
    @ParameterizedTest
    @CsvSource({"-1, 20", "0, 0", "0, 101"})
    void rejectsANumberOrSizeOutsideTheLimits(int number, int size) {
        assertThrows(InvalidValue.class, () -> new PageRequest(number, size));
    }

    @Test
    void skipsTheItemsOfThePreviousPages() {
        assertEquals(75, new PageRequest(3, 25).offset());
    }

    @Test
    void computesTheOffsetOfADistantPageWithoutOverflow() {
        assertEquals(214_748_364_700L, new PageRequest(Integer.MAX_VALUE, 100).offset());
    }
}
