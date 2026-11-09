package edu.itba.fieldops.adapters;

import edu.itba.fieldops.adapters.event.DeliveryProgress;
import edu.itba.fieldops.domain.event.Delivery;
import edu.itba.fieldops.domain.event.DeliveryFailure;
import edu.itba.fieldops.domain.event.EventDeliveries;
import edu.itba.fieldops.domain.event.EventFilter;
import edu.itba.fieldops.domain.event.Events;
import edu.itba.fieldops.domain.event.PendingDelivery;
import edu.itba.fieldops.domain.event.RecordedEvent;
import edu.itba.fieldops.domain.identity.EventId;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import edu.itba.fieldops.domain.shared.UnknownResource;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;

@Repository
@Profile(PersistenceProfiles.MEMORY)
public class InMemoryEventDeliveries implements EventDeliveries {
    private static final Comparator<Entry> IN_SEQUENCE =
            Comparator.comparingLong(Entry::sequence).thenComparing(Entry::consumer);

    private final Events events;
    private final Set<EventId> dispatched = new HashSet<>();
    private final Map<Key, Entry> deliveries = new LinkedHashMap<>();

    public InMemoryEventDeliveries(Events events) {
        this.events = Objects.requireNonNull(events, "events");
    }

    @Override
    public synchronized List<RecordedEvent> undispatched(int limit) {
        List<RecordedEvent> found = new ArrayList<>();
        int number = 0;
        Page<RecordedEvent> page;
        do {
            page = events.page(EventFilter.all(), new PageRequest(number++, PageRequest.MAX_SIZE));
            page.items().stream().filter(item -> !dispatched.contains(item.id())).forEach(found::add);
        } while (found.size() < limit && number < page.totalPages());
        return found.stream().limit(limit).toList();
    }

    @Override
    public synchronized void schedule(EventId id, Set<String> consumers, Instant at) {
        long sequence = events.require(id).sequence();
        if (dispatched.add(id)) {
            consumers.forEach(consumer -> deliveries.put(
                    new Key(id, consumer), new Entry(id, consumer, sequence, DeliveryProgress.pendingFrom(at))
            ));
        }
    }

    @Override
    public synchronized List<PendingDelivery> due(Instant now, int limit) {
        return deliveries.values().stream()
                .filter(entry -> entry.progress().isDue(now))
                .sorted(IN_SEQUENCE)
                .limit(limit)
                .map(entry -> new PendingDelivery(events.require(entry.event()), entry.consumer(), entry.progress().attempts()))
                .toList();
    }

    @Override
    public synchronized void succeeded(EventId id, String consumer, Instant at) {
        advance(id, consumer, progress -> progress.delivered(at));
    }

    @Override
    public synchronized void failed(EventId id, String consumer, DeliveryFailure failure) {
        advance(id, consumer, progress -> progress.failed(failure));
    }

    @Override
    public synchronized void retry(EventId id, String consumer, Instant at) {
        advance(id, consumer, progress -> progress.retried(at));
    }

    @Override
    public synchronized List<Delivery> of(EventId id) {
        return deliveries.values().stream()
                .filter(entry -> entry.event().equals(id))
                .sorted(IN_SEQUENCE)
                .map(entry -> entry.progress().toDelivery(entry.consumer()))
                .toList();
    }

    private void advance(EventId id, String consumer, UnaryOperator<DeliveryProgress> transition) {
        Key key = new Key(id, consumer);
        Entry entry = deliveries.get(key);
        if (entry == null) {
            throw new UnknownResource("delivery", consumer);
        }
        deliveries.put(key, new Entry(id, consumer, entry.sequence(), transition.apply(entry.progress())));
    }

    private record Key(EventId event, String consumer) {
    }

    private record Entry(EventId event, String consumer, long sequence, DeliveryProgress progress) {
    }
}
