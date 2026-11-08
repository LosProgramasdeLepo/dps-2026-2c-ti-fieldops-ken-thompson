package edu.itba.fieldops.domain.event;

import java.util.List;

public interface EventLog {
    void append(List<DomainEvent> events);
}
