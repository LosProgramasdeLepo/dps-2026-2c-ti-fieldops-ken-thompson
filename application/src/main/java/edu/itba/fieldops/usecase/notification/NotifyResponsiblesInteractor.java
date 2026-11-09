package edu.itba.fieldops.usecase.notification;

import edu.itba.fieldops.domain.event.DomainEvent;
import edu.itba.fieldops.domain.event.RecordedEvent;
import edu.itba.fieldops.domain.notification.Notifier;
import edu.itba.fieldops.domain.notification.ResponsiblesToNotify;
import edu.itba.fieldops.domain.shared.Clock;
import edu.itba.fieldops.domain.usecase.notification.NotifyResponsibles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class NotifyResponsiblesInteractor implements NotifyResponsibles {
    private final ResponsiblesToNotify responsibles;
    private final Notifier notifier;
    private final Clock clock;

    public NotifyResponsiblesInteractor(ResponsiblesToNotify responsibles, Notifier notifier, Clock clock) {
        this.responsibles = Objects.requireNonNull(responsibles, "responsibles to notify");
        this.notifier = Objects.requireNonNull(notifier, "notifier");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public boolean concerns(DomainEvent event) {
        return responsibles.concern(event);
    }

    @Override
    @Transactional
    public void about(RecordedEvent event) {
        responsibles.notificationsAbout(event, clock.now()).forEach(notifier::send);
    }
}
