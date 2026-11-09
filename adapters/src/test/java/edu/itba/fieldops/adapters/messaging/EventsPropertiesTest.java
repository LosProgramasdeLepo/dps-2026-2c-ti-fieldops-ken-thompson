package edu.itba.fieldops.adapters.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventsPropertiesTest {
    private static EventsProperties bind(Map<String, String> values) {
        return new Binder(new MapConfigurationPropertySource(values)).bindOrCreate("fieldops.events", EventsProperties.class);
    }

    @Test
    void hasTheDocumentedDefaults() {
        assertEquals(new EventsProperties(50, 5, Duration.ofSeconds(1), Duration.ofMinutes(1)), bind(Map.of()));
    }

    @Test
    void bindsTheConfiguredValues() {
        assertEquals(
                new EventsProperties(10, 2, Duration.ofMillis(5), Duration.ofMillis(50)),
                bind(Map.of(
                        "fieldops.events.batch-size", "10",
                        "fieldops.events.max-attempts", "2",
                        "fieldops.events.backoff-base", "5ms",
                        "fieldops.events.backoff-max", "50ms"
                ))
        );
    }

    @Test
    void rejectsABatchBelowOne() {
        assertThrows(BindException.class, () -> bind(Map.of("fieldops.events.batch-size", "0")));
    }

    @Test
    void maxAttemptsBelowOneCannotBuildAPolicy() {
        EventsProperties properties = bind(Map.of("fieldops.events.max-attempts", "0"));

        assertThrows(IllegalArgumentException.class, properties::retryPolicy);
    }
}
