package edu.itba.fieldops.usecase.shared;

import edu.itba.fieldops.domain.shared.InvalidValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PageTest {
    @ParameterizedTest
    @CsvSource({"0, 10, 0", "1, 10, 1", "10, 10, 1", "11, 10, 2", "100, 10, 10"})
    void countsThePagesThatHoldEveryItem(long totalItems, int size, int pages) {
        assertEquals(pages, new Page<>(List.of(), new PageRequest(0, size), totalItems).totalPages());
    }

    @Test
    void rejectsMoreItemsThanThePageSize() {
        PageRequest request = new PageRequest(0, 1);

        assertThrows(InvalidValue.class, () -> new Page<>(List.of("Ada", "Bob"), request, 2));
    }

    @Test
    void rejectsATotalSmallerThanThePage() {
        PageRequest request = new PageRequest(0, 2);

        assertThrows(InvalidValue.class, () -> new Page<>(List.of("Ada", "Bob"), request, 1));
    }

    @Test
    void mapsItsItemsAndKeepsItsPosition() {
        PageRequest request = new PageRequest(1, 2);

        Page<Integer> lengths = new Page<>(List.of("Ada", "Eve"), request, 4).map(String::length);

        assertEquals(new Page<>(List.of(3, 3), request, 4), lengths);
    }
}
