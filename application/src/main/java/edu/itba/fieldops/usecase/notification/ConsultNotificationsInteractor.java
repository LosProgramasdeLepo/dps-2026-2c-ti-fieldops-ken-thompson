package edu.itba.fieldops.usecase.notification;

import edu.itba.fieldops.domain.catalog.PersonRegistry;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.notification.Notification;
import edu.itba.fieldops.domain.notification.Notifications;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import edu.itba.fieldops.domain.usecase.notification.ConsultNotifications;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class ConsultNotificationsInteractor implements ConsultNotifications {
    private final PersonRegistry people;
    private final Notifications notifications;

    public ConsultNotificationsInteractor(PersonRegistry people, Notifications notifications) {
        this.people = Objects.requireNonNull(people, "people");
        this.notifications = Objects.requireNonNull(notifications, "notifications");
    }

    @Override
    public Page<Notification> of(PersonId recipient, PageRequest request) {
        people.require(recipient);
        return notifications.of(recipient, request);
    }
}
