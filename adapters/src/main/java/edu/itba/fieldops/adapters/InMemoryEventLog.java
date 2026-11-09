package edu.itba.fieldops.adapters;

import edu.itba.fieldops.domain.event.DomainEvent;
import edu.itba.fieldops.domain.event.EventFilter;
import edu.itba.fieldops.domain.event.EventLog;
import edu.itba.fieldops.domain.event.RecordedEvent;
import edu.itba.fieldops.domain.identity.EventId;
import edu.itba.fieldops.domain.shared.Clock;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@Profile(PersistenceProfiles.MEMORY)
public class InMemoryEventLog implements EventLog {
    private final List<RecordedEvent> recorded = new ArrayList<>();
    private final AtomicLong sequence = new AtomicLong();
    private final Clock clock;

    public InMemoryEventLog(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public synchronized void append(List<DomainEvent> events) {
        for (DomainEvent event : List.copyOf(Objects.requireNonNull(events, "events"))) {
            recorded.add(new RecordedEvent(new EventId(UUID.randomUUID()), sequence.incrementAndGet(), clock.now(), event));
        }
    }

    @Override
    public synchronized Page<RecordedEvent> page(EventFilter filter, PageRequest request) {
        Objects.requireNonNull(filter, "filter");
        List<RecordedEvent> matching = recorded.stream()
                .filter(item -> filter.expeditions().isEmpty() || filter.expeditions().contains(item.event().expeditionId()))
                .filter(item -> filter.types().isEmpty() || filter.types().contains(item.event().type()))
                .toList();
        return InMemoryPages.slice(matching, request);
    }

    @Override
    public synchronized Optional<RecordedEvent> find(EventId id) {
        Objects.requireNonNull(id, "event id");
        return recorded.stream().filter(item -> item.id().equals(id)).findFirst();
    }
}
