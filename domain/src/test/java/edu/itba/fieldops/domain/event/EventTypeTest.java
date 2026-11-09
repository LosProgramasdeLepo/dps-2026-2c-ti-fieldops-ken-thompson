package edu.itba.fieldops.domain.event;

import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.shared.InvalidValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventTypeTest {
    @Test
    void namesTheTypeOfAnEvent() {
        RunStarted started = new RunStarted(new ExpeditionId(UUID.randomUUID()), Instant.parse("2026-11-01T08:00:00Z"));

        assertEquals(new EventType("RunStarted"), EventType.of(started));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Nope", "runStarted"})
    void rejectsATypeTheDomainDoesNotKnow(String value) {
        assertThrows(InvalidValue.class, () -> new EventType(value));
    }
}
