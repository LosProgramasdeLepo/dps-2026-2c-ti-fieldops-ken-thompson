package edu.itba.fieldops.adapters.jpa.event;

import edu.itba.fieldops.adapters.PersistenceProfiles;
import edu.itba.fieldops.domain.event.DomainEvent;
import edu.itba.fieldops.domain.event.EventFilter;
import edu.itba.fieldops.domain.event.EventLog;
import edu.itba.fieldops.domain.event.RecordedEvent;
import edu.itba.fieldops.domain.identity.EventId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.shared.Clock;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
@Profile(PersistenceProfiles.SQL)
@Transactional(readOnly = true)
public class JpaEventLog implements EventLog {
    private static final Sort IN_SEQUENCE = Sort.by("sequence");

    private final EventJpaRepository events;
    private final Clock clock;

    public JpaEventLog(EventJpaRepository events, Clock clock) {
        this.events = Objects.requireNonNull(events, "events");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void append(List<DomainEvent> appended) {
        Instant recordedAt = clock.now();
        events.saveAll(List.copyOf(Objects.requireNonNull(appended, "events")).stream()
                .map(event -> new EventEntity(UUID.randomUUID(), event, recordedAt, EventPayloads.encode(event)))
                .toList());
    }

    @Override
    public Page<RecordedEvent> page(EventFilter filter, PageRequest request) {
        Objects.requireNonNull(filter, "filter");
        org.springframework.data.domain.Page<EventEntity> stored = events.findAll(
                matching(filter),
                org.springframework.data.domain.PageRequest.of(request.number(), request.size(), IN_SEQUENCE)
        );
        return new Page<>(stored.getContent().stream().map(EventPayloads::decode).toList(), request, stored.getTotalElements());
    }

    @Override
    public Optional<RecordedEvent> find(EventId id) {
        return events.findById(Objects.requireNonNull(id, "event id").value()).map(EventPayloads::decode);
    }

    private static Specification<EventEntity> matching(EventFilter filter) {
        return (root, query, builder) -> {
            List<Predicate> conditions = new ArrayList<>();
            if (!filter.expeditions().isEmpty()) {
                conditions.add(root.get("expeditionId").in(filter.expeditions().stream().map(ExpeditionId::value).toList()));
            }
            if (!filter.types().isEmpty()) {
                conditions.add(root.get("type").in(filter.types()));
            }
            return builder.and(conditions.toArray(Predicate[]::new));
        };
    }
}
