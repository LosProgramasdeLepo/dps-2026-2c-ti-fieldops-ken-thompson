package edu.itba.fieldops.adapters.jpa.notification;

import edu.itba.fieldops.domain.event.EventType;
import edu.itba.fieldops.domain.identity.EventId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.notification.Notification;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class NotificationEntity {
    @EmbeddedId
    private NotificationKey id;

    @Column(nullable = false)
    private String type;

    @Column(name = "expedition_id", nullable = false)
    private UUID expeditionId;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Column(name = "registration_order", insertable = false, updatable = false)
    private Long registrationOrder;

    protected NotificationEntity() {
    }

    private NotificationEntity(NotificationKey id, String type, UUID expeditionId, Instant sentAt) {
        this.id = id;
        this.type = type;
        this.expeditionId = expeditionId;
        this.sentAt = sentAt;
    }

    static NotificationKey keyOf(Notification notification) {
        return new NotificationKey(notification.about().value(), notification.recipient().value());
    }

    static NotificationEntity of(Notification notification) {
        return new NotificationEntity(
                keyOf(notification),
                notification.type().value(),
                notification.expeditionId().value(),
                notification.sentAt()
        );
    }

    Notification toDomain() {
        return new Notification(
                new EventId(id.eventId()),
                new PersonId(id.personId()),
                new EventType(type),
                new ExpeditionId(expeditionId),
                sentAt
        );
    }
}
