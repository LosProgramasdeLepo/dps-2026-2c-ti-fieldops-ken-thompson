package edu.itba.fieldops.adapters;

import edu.itba.fieldops.domain.event.DomainEvent;
import edu.itba.fieldops.domain.event.EventLog;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Provisional event log, kept in memory for every persistence profile. The events of the {@code sql} and
 * {@code mongo} profiles are not durable until phase 2 replaces it with a transactional implementation.
 */
@Repository
public class InMemoryEventLog implements EventLog {
    private final List<DomainEvent> events = new ArrayList<>();

    @Override
    public synchronized void append(List<DomainEvent> appended) {
        events.addAll(List.copyOf(Objects.requireNonNull(appended, "events")));
    }

    public synchronized List<DomainEvent> events() {
        return List.copyOf(events);
    }
}
