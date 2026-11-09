package edu.itba.fieldops.adapters;

import edu.itba.fieldops.adapters.notification.NotificationInbox;
import edu.itba.fieldops.domain.event.EventLog;
import edu.itba.fieldops.domain.identity.PersonId;

import java.time.Instant;
import java.util.UUID;

class InMemoryNotificationInboxTest extends NotificationInboxContract {
    private final InMemoryEventLog log = new InMemoryEventLog(() -> Instant.parse("2026-11-01T08:00:00Z"));
    private final InMemoryNotificationInbox inbox = new InMemoryNotificationInbox();

    @Override
    protected NotificationInbox inbox() {
        return inbox;
    }

    @Override
    protected EventLog eventLog() {
        return log;
    }

    @Override
    protected PersonId registeredPerson() {
        return new PersonId(UUID.randomUUID());
    }
}
