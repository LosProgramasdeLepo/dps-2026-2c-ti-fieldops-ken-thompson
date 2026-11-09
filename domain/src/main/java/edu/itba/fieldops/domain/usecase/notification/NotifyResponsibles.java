package edu.itba.fieldops.domain.usecase.notification;

import edu.itba.fieldops.domain.event.DomainEvent;
import edu.itba.fieldops.domain.event.RecordedEvent;

public interface NotifyResponsibles {
    boolean concerns(DomainEvent event);

    void about(RecordedEvent event);
}
