package edu.itba.fieldops.domain.itinerary;

import edu.itba.fieldops.domain.shared.InvalidValue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertThrows;

class DelayTest {
    @ParameterizedTest
    @ValueSource(strings = {"PT0S", "-PT1H"})
    void rejectsADelayThatDoesNotPostpone(String duration) {
        assertThrows(InvalidValue.class, () -> new Delay(Duration.parse(duration)));
    }
}
