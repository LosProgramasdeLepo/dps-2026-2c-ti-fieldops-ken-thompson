package edu.itba.fieldops.api.notification;

import java.time.Instant;
import java.util.UUID;

public final class NotificationResponses {
    private NotificationResponses() {
    }

    public record NotificationResponse(UUID eventId, String type, UUID expeditionId, Instant sentAt) {
    }
}
