package edu.itba.fieldops.api.notification;

import edu.itba.fieldops.api.json.PageQuery;
import edu.itba.fieldops.api.json.PageResponse;
import edu.itba.fieldops.api.json.Pages;
import edu.itba.fieldops.api.notification.NotificationResponses.NotificationResponse;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.usecase.notification.ConsultNotifications;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public final class NotificationsController {
    private final ConsultNotifications notifications;

    public NotificationsController(ConsultNotifications notifications) {
        this.notifications = notifications;
    }

    @GetMapping("/v1/people/{id}/notifications")
    public ResponseEntity<PageResponse<NotificationResponse>> of(@PathVariable UUID id, @Valid PageQuery page) {
        return Pages.toResponse(notifications.of(new PersonId(id), Pages.toRequest(page)), NotificationMapping::notification);
    }
}
