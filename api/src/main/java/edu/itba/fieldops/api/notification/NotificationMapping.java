package edu.itba.fieldops.api.notification;

import edu.itba.fieldops.api.notification.NotificationResponses.NotificationResponse;
import edu.itba.fieldops.domain.notification.Notification;

final class NotificationMapping {
    private NotificationMapping() {
    }

    static NotificationResponse notification(Notification notification) {
        return new NotificationResponse(
                notification.about().value(),
                notification.type().value(),
                notification.expeditionId().value(),
                notification.sentAt()
        );
    }
}
