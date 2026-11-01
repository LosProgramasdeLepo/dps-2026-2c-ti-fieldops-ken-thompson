package edu.itba.fieldops.domain.shared;

import edu.itba.fieldops.domain.catalog.CertificationName;
import edu.itba.fieldops.domain.catalog.ConsumableName;
import edu.itba.fieldops.domain.catalog.PersonName;
import edu.itba.fieldops.domain.expedition.Justification;
import edu.itba.fieldops.domain.itinerary.ActivityName;
import edu.itba.fieldops.domain.tracking.IncidentDescription;
import edu.itba.fieldops.domain.tracking.ObservationText;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TextValuesTest {
    static Stream<Arguments> blankTexts() {
        return Stream.of(
                Arguments.of("person name", (Function<String, ?>) PersonName::new, "  "),
                Arguments.of("certification name", (Function<String, ?>) CertificationName::new, ""),
                Arguments.of("consumable name", (Function<String, ?>) ConsumableName::new, null),
                Arguments.of("activity name", (Function<String, ?>) ActivityName::new, "\t"),
                Arguments.of("incident", (Function<String, ?>) IncidentDescription::new, " "),
                Arguments.of("observation", (Function<String, ?>) ObservationText::new, ""),
                Arguments.of("justification", (Function<String, ?>) Justification::new, null)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("blankTexts")
    void rejectsABlankText(String label, Function<String, ?> create, String blank) {
        InvalidValue rejected = assertThrows(InvalidValue.class, () -> create.apply(blank));

        assertEquals(label + " must not be blank", rejected.getMessage());
    }
}
