package edu.itba.fieldops.adapters.notification;

import edu.itba.fieldops.domain.event.EventType;
import edu.itba.fieldops.domain.identity.EventId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.notification.Notification;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StoredNotifierTest {
    private static final PersonId ADA = new PersonId(UUID.randomUUID());
    private static final Notification APPROVED = new Notification(
            new EventId(UUID.randomUUID()),
            ADA,
            new EventType("ExpeditionApproved"),
            new ExpeditionId(UUID.randomUUID()),
            Instant.parse("2026-11-01T08:00:00Z")
    );

    private final List<Notification> kept = new ArrayList<>();
    private final NotificationProperties properties = new NotificationProperties();
    private final StoredNotifier notifier = new StoredNotifier(new NotificationInbox() {
        @Override
        public void keep(Notification notification) {
            kept.add(notification);
        }

        @Override
        public Page<Notification> of(PersonId recipient, PageRequest request) {
            return new Page<>(kept, request, kept.size());
        }
    }, properties);

    @Test
    void sendingKeepsTheNotificationInTheInbox() {
        notifier.send(APPROVED);

        assertEquals(List.of(APPROVED), notifier.of(ADA, new PageRequest(0, 20)).items());
    }

    @Test
    void aSimulatedFailureKeepsNothing() {
        properties.setSimulateFailure(true);

        assertThrows(SimulatedNotificationFailure.class, () -> notifier.send(APPROVED));
        assertEquals(List.of(), kept);
    }

    @Test
    void sendingWorksAgainOnceTheFailureIsTurnedOff() {
        properties.setSimulateFailure(true);
        properties.setSimulateFailure(false);

        notifier.send(APPROVED);

        assertEquals(List.of(APPROVED), kept);
    }
}
