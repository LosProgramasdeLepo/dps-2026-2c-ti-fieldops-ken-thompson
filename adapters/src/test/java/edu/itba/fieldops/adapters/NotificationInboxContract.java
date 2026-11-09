package edu.itba.fieldops.adapters;

import edu.itba.fieldops.adapters.notification.NotificationInbox;
import edu.itba.fieldops.domain.event.EventFilter;
import edu.itba.fieldops.domain.event.EventLog;
import edu.itba.fieldops.domain.event.EventType;
import edu.itba.fieldops.domain.event.RunStarted;
import edu.itba.fieldops.domain.identity.EventId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.notification.Notification;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class NotificationInboxContract {
    private static final Instant AT = Instant.parse("2026-11-01T08:00:00Z");
    private static final PageRequest FIRST_PAGE = new PageRequest(0, 20);

    protected abstract NotificationInbox inbox();

    protected abstract EventLog eventLog();

    protected abstract PersonId registeredPerson();

    protected void inTransaction(Runnable work) {
        work.run();
    }

    @Test
    void aRecipientSeesWhatWasSentToThem() {
        PersonId ada = registeredPerson();
        Notification notification = notification(recordedEvent(), ada, AT);

        keep(notification);

        assertEquals(List.of(notification), inbox().of(ada, FIRST_PAGE).items());
    }

    @Test
    void aSecondCopyOfTheSameNotificationIsIgnored() {
        PersonId ada = registeredPerson();
        EventId event = recordedEvent();
        Notification first = notification(event, ada, AT);

        keep(first);
        keep(notification(event, ada, AT.plus(Duration.ofMinutes(1))));

        Page<Notification> inbox = inbox().of(ada, FIRST_PAGE);
        assertEquals(List.of(first), inbox.items());
        assertEquals(1, inbox.totalItems());
    }

    @Test
    void theSameEventReachesEveryRecipient() {
        PersonId ada = registeredPerson();
        PersonId bob = registeredPerson();
        EventId event = recordedEvent();

        keep(notification(event, ada, AT));
        keep(notification(event, bob, AT));

        assertEquals(1, inbox().of(ada, FIRST_PAGE).totalItems());
        assertEquals(1, inbox().of(bob, FIRST_PAGE).totalItems());
    }

    @Test
    void aRecipientDoesNotSeeTheNotificationsOfOthers() {
        PersonId ada = registeredPerson();
        keep(notification(recordedEvent(), ada, AT));

        assertEquals(0, inbox().of(registeredPerson(), FIRST_PAGE).totalItems());
    }

    @Test
    void theInboxListsTheNewestFirstByPages() {
        PersonId ada = registeredPerson();
        Notification oldest = notification(recordedEvent(), ada, AT);
        Notification middle = notification(recordedEvent(), ada, AT);
        Notification newest = notification(recordedEvent(), ada, AT);
        keep(oldest);
        keep(middle);
        keep(newest);

        Page<Notification> first = inbox().of(ada, new PageRequest(0, 2));
        Page<Notification> second = inbox().of(ada, new PageRequest(1, 2));

        assertEquals(List.of(newest, middle), first.items());
        assertEquals(List.of(oldest), second.items());
        assertEquals(3, first.totalItems());
    }

    private void keep(Notification notification) {
        inTransaction(() -> inbox().keep(notification));
    }

    private EventId recordedEvent() {
        ExpeditionId expeditionId = new ExpeditionId(UUID.randomUUID());
        inTransaction(() -> eventLog().append(List.of(new RunStarted(expeditionId, AT))));
        return eventLog().page(EventFilter.forExpeditions(Set.of(expeditionId)), new PageRequest(0, 1)).items().getFirst().id();
    }

    private static Notification notification(EventId event, PersonId recipient, Instant sentAt) {
        return new Notification(event, recipient, new EventType("ExpeditionApproved"), new ExpeditionId(UUID.randomUUID()), sentAt);
    }
}
