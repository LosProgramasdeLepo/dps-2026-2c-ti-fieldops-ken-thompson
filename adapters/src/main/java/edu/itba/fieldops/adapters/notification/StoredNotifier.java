package edu.itba.fieldops.adapters.notification;

import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.notification.Notification;
import edu.itba.fieldops.domain.notification.Notifications;
import edu.itba.fieldops.domain.notification.Notifier;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class StoredNotifier implements Notifier, Notifications {
    private final NotificationInbox inbox;
    private final NotificationProperties properties;

    public StoredNotifier(NotificationInbox inbox, NotificationProperties properties) {
        this.inbox = Objects.requireNonNull(inbox, "inbox");
        this.properties = Objects.requireNonNull(properties, "notification properties");
    }

    @Override
    public void send(Notification notification) {
        Objects.requireNonNull(notification, "notification");
        if (properties.isSimulateFailure()) {
            throw new SimulatedNotificationFailure();
        }
        inbox.keep(notification);
    }

    @Override
    public Page<Notification> of(PersonId recipient, PageRequest request) {
        return inbox.of(recipient, request);
    }
}
