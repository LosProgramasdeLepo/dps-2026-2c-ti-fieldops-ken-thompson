package edu.itba.fieldops.domain.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PendingEvents {
    private final List<DomainEvent> events = new ArrayList<>();

    public void record(DomainEvent event) {
        events.add(Objects.requireNonNull(event, "event"));
    }

    public List<DomainEvent> drain() {
        List<DomainEvent> drained = List.copyOf(events);
        events.clear();
        return drained;
    }
}
