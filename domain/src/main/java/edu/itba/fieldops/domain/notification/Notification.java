package edu.itba.fieldops.domain.notification;

import edu.itba.fieldops.domain.event.EventType;
import edu.itba.fieldops.domain.identity.EventId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;

import java.time.Instant;
import java.util.Objects;

public record Notification(EventId about, PersonId recipient, EventType type, ExpeditionId expeditionId, Instant sentAt) {
    public Notification {
        Objects.requireNonNull(about, "event id");
        Objects.requireNonNull(recipient, "recipient");
        Objects.requireNonNull(type, "event type");
        Objects.requireNonNull(expeditionId, "expedition id");
        Objects.requireNonNull(sentAt, "sent at");
    }
}
