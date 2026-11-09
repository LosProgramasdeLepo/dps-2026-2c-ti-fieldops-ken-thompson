package edu.itba.fieldops.adapters.messaging;

import edu.itba.fieldops.domain.event.DomainEvent;
import edu.itba.fieldops.domain.event.ExpeditionApproved;
import edu.itba.fieldops.domain.event.RecordedEvent;
import edu.itba.fieldops.domain.event.RunStarted;
import edu.itba.fieldops.domain.identity.EventId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.usecase.notification.NotifyResponsibles;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotifyResponsiblesConsumerTest {
    private static final Instant AT = Instant.parse("2026-11-01T08:00:00Z");
    private static final ExpeditionId PLAN = new ExpeditionId(UUID.randomUUID());
    private static final ExpeditionApproved APPROVED = new ExpeditionApproved(PLAN, AT, 1, Optional.empty());

    private final List<RecordedEvent> notified = new ArrayList<>();
    private final NotifyResponsiblesConsumer consumer = new NotifyResponsiblesConsumer(new NotifyResponsibles() {
        @Override
        public boolean concerns(DomainEvent event) {
            return event.equals(APPROVED);
        }

        @Override
        public void about(RecordedEvent event) {
            notified.add(event);
        }
    });

    @Test
    void subscribesToWhatTheUseCaseConcerns() {
        assertTrue(consumer.subscribesTo(APPROVED));
    }

    @Test
    void ignoresWhatTheUseCaseDoesNotConcern() {
        assertFalse(consumer.subscribesTo(new RunStarted(PLAN, AT)));
    }

    @Test
    void passesTheRecordedEventToTheUseCase() {
        RecordedEvent approved = new RecordedEvent(new EventId(UUID.randomUUID()), 1, AT, APPROVED);

        consumer.consume(approved);

        assertEquals(List.of(approved), notified);
    }

    @Test
    void hasAStableName() {
        assertEquals("notify-responsibles", consumer.name());
    }
}
