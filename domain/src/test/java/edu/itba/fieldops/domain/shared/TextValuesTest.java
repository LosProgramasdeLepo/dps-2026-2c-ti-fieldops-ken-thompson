package edu.itba.fieldops.domain.shared;

import edu.itba.fieldops.domain.catalog.CertificationName;
import edu.itba.fieldops.domain.catalog.ConsumableName;
import edu.itba.fieldops.domain.catalog.PersonName;
import edu.itba.fieldops.domain.expedition.Justification;
import edu.itba.fieldops.domain.expedition.Objective;
import edu.itba.fieldops.domain.expedition.Restriction;
import edu.itba.fieldops.domain.itinerary.ActivityName;
import edu.itba.fieldops.domain.tracking.IncidentDescription;
import edu.itba.fieldops.domain.tracking.ObservationText;
import edu.itba.fieldops.domain.tracking.Outcome;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TextValuesTest {
    static Stream<Arguments> texts() {
        return Stream.of(
                text("person name", PersonName::new, PersonName::value),
                text("certification name", CertificationName::new, CertificationName::value),
                text("consumable name", ConsumableName::new, ConsumableName::value),
                text("activity name", ActivityName::new, ActivityName::value),
                text("incident", IncidentDescription::new, IncidentDescription::text),
                text("observation", ObservationText::new, ObservationText::text),
                text("result", Outcome::new, Outcome::text),
                text("justification", Justification::new, Justification::text),
                text("objective", Objective::new, Objective::text),
                text("restriction", Restriction::new, Restriction::text),
                text("zone name", WorkZone::new, WorkZone::name)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("texts")
    void rejectsABlankText(String label, Function<String, Object> create, Function<Object, String> read) {
        for (String blank : new String[]{"", "   ", "\t\n"}) {
            InvalidValue rejected = assertThrows(InvalidValue.class, () -> create.apply(blank));
            assertEquals(label + " must not be blank", rejected.getMessage());
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("texts")
    void rejectsAMissingText(String label, Function<String, Object> create, Function<Object, String> read) {
        assertThrows(InvalidValue.class, () -> create.apply(null));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("texts")
    void keepsTheTextTrimmed(String label, Function<String, Object> create, Function<Object, String> read) {
        assertEquals("Delta site", read.apply(create.apply("  Delta site ")));
    }

    @SuppressWarnings("unchecked")
    private static <T> Arguments text(String label, Function<String, T> create, Function<T, String> read) {
        return Arguments.of(label, create, (Function<Object, String>) value -> read.apply((T) value));
    }
}
