package edu.itba.fieldops.adapters.jpa.notification;

import edu.itba.fieldops.adapters.PersistenceProfiles;
import edu.itba.fieldops.adapters.jpa.JpaPages;
import edu.itba.fieldops.adapters.notification.NotificationInbox;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.notification.Notification;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Repository
@Profile(PersistenceProfiles.SQL)
@Transactional(readOnly = true)
public class JpaNotificationInbox implements NotificationInbox {
    private final NotificationJpaRepository notifications;

    public JpaNotificationInbox(NotificationJpaRepository notifications) {
        this.notifications = Objects.requireNonNull(notifications, "notifications");
    }

    @Override
    @Transactional
    public void keep(Notification notification) {
        Objects.requireNonNull(notification, "notification");
        if (!notifications.existsById(NotificationEntity.keyOf(notification))) {
            notifications.save(NotificationEntity.of(notification));
        }
    }

    @Override
    public Page<Notification> of(PersonId recipient, PageRequest request) {
        Objects.requireNonNull(recipient, "recipient");
        return JpaPages.page(
                notifications.findOf(recipient.value(), JpaPages.unsorted(request)),
                request,
                NotificationEntity::toDomain
        );
    }
}
