package edu.itba.fieldops.adapters.jpa.event;

import edu.itba.fieldops.adapters.PersistenceProfiles;
import edu.itba.fieldops.adapters.event.DeliveryProgress;
import edu.itba.fieldops.domain.event.Delivery;
import edu.itba.fieldops.domain.event.DeliveryFailure;
import edu.itba.fieldops.domain.event.EventDeliveries;
import edu.itba.fieldops.domain.event.PendingDelivery;
import edu.itba.fieldops.domain.event.RecordedEvent;
import edu.itba.fieldops.domain.identity.EventId;
import edu.itba.fieldops.domain.shared.UnknownResource;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

@Repository
@Profile(PersistenceProfiles.SQL)
@Transactional(readOnly = true)
public class JpaEventDeliveries implements EventDeliveries {
    private final EventJpaRepository events;
    private final EventFanoutJpaRepository fanouts;
    private final EventDeliveryJpaRepository deliveries;

    public JpaEventDeliveries(
            EventJpaRepository events,
            EventFanoutJpaRepository fanouts,
            EventDeliveryJpaRepository deliveries
    ) {
        this.events = Objects.requireNonNull(events, "events");
        this.fanouts = Objects.requireNonNull(fanouts, "fanouts");
        this.deliveries = Objects.requireNonNull(deliveries, "deliveries");
    }

    @Override
    public List<RecordedEvent> undispatched(int limit) {
        return events.findUndispatched(Limit.of(limit)).stream().map(EventPayloads::decode).toList();
    }

    @Override
    @Transactional
    public void schedule(EventId id, Set<String> consumers, Instant at) {
        UUID eventId = Objects.requireNonNull(id, "event id").value();
        if (fanouts.existsById(eventId)) {
            return;
        }
        fanouts.save(new EventFanoutEntity(eventId, at));
        deliveries.saveAll(consumers.stream()
                .map(consumer -> new EventDeliveryEntity(new DeliveryKey(eventId, consumer), DeliveryProgress.pendingFrom(at)))
                .toList());
    }

    @Override
    public List<PendingDelivery> due(Instant now, int limit) {
        List<EventDeliveryEntity> due = deliveries.findDue(now, Limit.of(limit));
        Map<UUID, RecordedEvent> recorded = events.findAllById(due.stream().map(item -> item.id().eventId()).toList()).stream()
                .map(EventPayloads::decode)
                .collect(Collectors.toMap(event -> event.id().value(), Function.identity()));
        return due.stream()
                .map(item -> new PendingDelivery(recorded.get(item.id().eventId()), item.id().consumer(), item.progress().attempts()))
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void succeeded(EventId id, String consumer, Instant at) {
        advance(id, consumer, progress -> progress.delivered(at));
    }

    @Override
    @Transactional
    public void failed(EventId id, String consumer, DeliveryFailure failure) {
        advance(id, consumer, progress -> progress.failed(failure));
    }

    @Override
    @Transactional
    public void retry(EventId id, String consumer, Instant at) {
        advance(id, consumer, progress -> progress.retried(at));
    }

    @Override
    public List<Delivery> of(EventId id) {
        return deliveries.findOf(Objects.requireNonNull(id, "event id").value()).stream()
                .map(item -> item.progress().toDelivery(item.id().consumer()))
                .toList();
    }

    private void advance(EventId id, String consumer, UnaryOperator<DeliveryProgress> transition) {
        EventDeliveryEntity delivery = deliveries.findById(new DeliveryKey(id.value(), consumer))
                .orElseThrow(() -> new UnknownResource("delivery", consumer));
        delivery.advanceTo(transition.apply(delivery.progress()));
        deliveries.save(delivery);
    }
}
