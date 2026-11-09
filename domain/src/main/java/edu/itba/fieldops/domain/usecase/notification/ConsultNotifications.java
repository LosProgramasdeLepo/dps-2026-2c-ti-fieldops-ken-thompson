package edu.itba.fieldops.domain.usecase.notification;

import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.notification.Notification;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;

public interface ConsultNotifications {
    Page<Notification> of(PersonId recipient, PageRequest request);
}
