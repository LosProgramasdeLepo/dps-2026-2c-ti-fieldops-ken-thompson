package edu.itba.fieldops.adapters.mongo.notification;

import edu.itba.fieldops.domain.event.EventType;
import edu.itba.fieldops.domain.identity.EventId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.notification.Notification;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = NotificationDocument.COLLECTION)
record NotificationDocument(@Id Key id, String type, String expeditionId, Instant sentAt, long registrationOrder) {
    static final String COLLECTION = "notifications";

    static Key keyOf(Notification notification) {
        return new Key(notification.about().value().toString(), notification.recipient().value().toString());
    }

    static NotificationDocument of(Notification notification, long registrationOrder) {
        return new NotificationDocument(
                keyOf(notification),
                notification.type().value(),
                notification.expeditionId().value().toString(),
                notification.sentAt(),
                registrationOrder
        );
    }

    Notification toDomain() {
        return new Notification(
                new EventId(UUID.fromString(id.eventId())),
                new PersonId(UUID.fromString(id.personId())),
                new EventType(type),
                new ExpeditionId(UUID.fromString(expeditionId)),
                sentAt
        );
    }

    record Key(String eventId, String personId) {
    }
}
