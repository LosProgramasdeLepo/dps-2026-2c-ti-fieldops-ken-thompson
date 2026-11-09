package edu.itba.fieldops.domain.event;

import edu.itba.fieldops.domain.identity.EventId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.shared.InvalidValue;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RecordedEventTest {
    private static final Instant AT = Instant.parse("2026-11-01T08:00:00Z");
    private static final EventId ID = new EventId(UUID.randomUUID());
    private static final DomainEvent EVENT = new RunStarted(new ExpeditionId(UUID.randomUUID()), AT);

    @Test
    void wrapsAnEventWithItsIdentityAndOrder() {
        RecordedEvent recorded = new RecordedEvent(ID, 7, AT, EVENT);

        assertAll(
                () -> assertEquals(ID, recorded.id()),
                () -> assertEquals(7, recorded.sequence()),
                () -> assertEquals(AT, recorded.recordedAt()),
                () -> assertEquals(EVENT, recorded.event())
        );
    }

    @Test
    void rejectsANonPositiveSequence() {
        assertAll(
                () -> assertThrows(InvalidValue.class, () -> new RecordedEvent(ID, 0, AT, EVENT)),
                () -> assertThrows(InvalidValue.class, () -> new RecordedEvent(ID, -1, AT, EVENT))
        );
    }

    @Test
    void rejectsNulls() {
        assertAll(
                () -> assertThrows(NullPointerException.class, () -> new RecordedEvent(null, 1, AT, EVENT)),
                () -> assertThrows(NullPointerException.class, () -> new RecordedEvent(ID, 1, null, EVENT)),
                () -> assertThrows(NullPointerException.class, () -> new RecordedEvent(ID, 1, AT, null))
        );
    }

    @Test
    void anEventIdRejectsNull() {
        assertThrows(NullPointerException.class, () -> new EventId(null));
    }
}
