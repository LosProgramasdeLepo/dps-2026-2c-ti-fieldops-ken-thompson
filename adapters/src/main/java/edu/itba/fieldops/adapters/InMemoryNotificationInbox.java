package edu.itba.fieldops.adapters;

import edu.itba.fieldops.adapters.notification.NotificationInbox;
import edu.itba.fieldops.domain.identity.EventId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.notification.Notification;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Repository
@Profile(PersistenceProfiles.MEMORY)
public class InMemoryNotificationInbox implements NotificationInbox {
    private final Map<Key, Notification> notifications = new LinkedHashMap<>();

    @Override
    public synchronized void keep(Notification notification) {
        Objects.requireNonNull(notification, "notification");
        notifications.putIfAbsent(new Key(notification.about(), notification.recipient()), notification);
    }

    @Override
    public synchronized Page<Notification> of(PersonId recipient, PageRequest request) {
        Objects.requireNonNull(recipient, "recipient");
        return InMemoryPages.slice(
                notifications.values().stream().filter(notification -> notification.recipient().equals(recipient)).toList().reversed(),
                request
        );
    }

    private record Key(EventId about, PersonId recipient) {
    }
}
