package edu.itba.fieldops.adapters;

import edu.itba.fieldops.usecase.shared.Page;
import edu.itba.fieldops.usecase.shared.PageRequest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InMemoryPagesTest {
    private static final List<String> NAMES = List.of("Ada", "Bob", "Eve", "Ian", "Kim");

    @ParameterizedTest
    @CsvSource({"0, 2, Ada Bob", "1, 2, Eve Ian", "2, 2, Kim", "3, 2, ''", "0, 100, Ada Bob Eve Ian Kim"})
    void slicesTheRequestedPageAndCountsEveryItem(int number, int size, String expected) {
        Page<String> page = InMemoryPages.slice(NAMES, new PageRequest(number, size));

        assertEquals(names(expected), page.items());
        assertEquals(NAMES.size(), page.totalItems());
    }

    private static List<String> names(String joined) {
        return Arrays.stream(joined.split(" ")).filter(name -> !name.isEmpty()).toList();
    }
}
