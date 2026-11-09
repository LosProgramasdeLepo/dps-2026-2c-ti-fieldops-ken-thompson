package edu.itba.fieldops.domain.event;

import java.util.List;

public interface EventLog extends Events {
    void append(List<DomainEvent> events);
}
