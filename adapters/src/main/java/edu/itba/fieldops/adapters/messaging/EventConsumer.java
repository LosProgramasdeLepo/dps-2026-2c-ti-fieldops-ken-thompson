package edu.itba.fieldops.adapters.messaging;

import edu.itba.fieldops.domain.event.DomainEvent;
import edu.itba.fieldops.domain.event.RecordedEvent;

public interface EventConsumer {
    String name();

    boolean subscribesTo(DomainEvent event);

    void consume(RecordedEvent event);
}
