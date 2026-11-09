package edu.itba.fieldops.domain.notification;

import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;

public interface Notifications {
    Page<Notification> of(PersonId recipient, PageRequest request);
}
