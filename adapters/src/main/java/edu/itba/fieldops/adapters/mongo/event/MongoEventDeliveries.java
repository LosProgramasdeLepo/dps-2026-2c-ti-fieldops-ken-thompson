package edu.itba.fieldops.adapters.mongo.event;

import edu.itba.fieldops.adapters.PersistenceProfiles;
import edu.itba.fieldops.adapters.event.DeliveryProgress;
import edu.itba.fieldops.adapters.event.EventPayloadCodec;
import edu.itba.fieldops.domain.event.Delivery;
import edu.itba.fieldops.domain.event.DeliveryFailure;
import edu.itba.fieldops.domain.event.DeliveryStatus;
import edu.itba.fieldops.domain.event.EventDeliveries;
import edu.itba.fieldops.domain.event.PendingDelivery;
import edu.itba.fieldops.domain.event.RecordedEvent;
import edu.itba.fieldops.domain.identity.EventId;
import edu.itba.fieldops.domain.shared.UnknownResource;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

@Repository
@Profile(PersistenceProfiles.MONGO)
@Transactional(readOnly = true)
public class MongoEventDeliveries implements EventDeliveries {
    private static final List<String> OPEN = List.of(DeliveryStatus.PENDING.name(), DeliveryStatus.RETRYING.name());

    private final EventMongoRepository events;
    private final FanoutMongoRepository fanouts;
    private final DeliveryMongoRepository deliveries;
    private final MongoOperations operations;
    private final EventPayloadCodec codec = new EventPayloadCodec();

    MongoEventDeliveries(
            EventMongoRepository events,
            FanoutMongoRepository fanouts,
            DeliveryMongoRepository deliveries,
            MongoOperations operations
    ) {
        this.events = Objects.requireNonNull(events, "events");
        this.fanouts = Objects.requireNonNull(fanouts, "fanouts");
        this.deliveries = Objects.requireNonNull(deliveries, "deliveries");
        this.operations = Objects.requireNonNull(operations, "operations");
        createIndexes();
    }

    @Override
    public List<RecordedEvent> undispatched(int limit) {
        Aggregation withoutFanout = Aggregation.newAggregation(
                Aggregation.sort(Sort.by("sequence")),
                Aggregation.lookup(FanoutDocument.COLLECTION, "_id", "_id", "fanout"),
                Aggregation.match(Criteria.where("fanout").size(0)),
                Aggregation.limit(limit)
        );
        return operations.aggregate(withoutFanout, EventDocument.COLLECTION, EventDocument.class).getMappedResults().stream()
                .map(document -> document.toDomain(codec))
                .toList();
    }

    @Override
    @Transactional
    public void schedule(EventId id, Set<String> consumers, Instant at) {
        String eventId = Objects.requireNonNull(id, "event id").value().toString();
        if (fanouts.existsById(eventId)) {
            return;
        }
        long sequence = events.findById(eventId).orElseThrow(() -> new UnknownResource("event", eventId)).sequence();
        fanouts.save(new FanoutDocument(eventId, at));
        deliveries.saveAll(consumers.stream()
                .map(consumer -> DeliveryDocument.of(new DeliveryDocument.Key(eventId, consumer), sequence, DeliveryProgress.pendingFrom(at)))
                .toList());
    }

    @Override
    public List<PendingDelivery> due(Instant now, int limit) {
        Query open = Query.query(Criteria.where("status").in(OPEN).and("nextAttemptAt").lte(now))
                .with(Sort.by("sequence", "id.consumer"))
                .limit(limit);
        List<DeliveryDocument> due = operations.find(open, DeliveryDocument.class);
        Map<String, RecordedEvent> recorded = events.findAllById(due.stream().map(item -> item.id().eventId()).toList()).stream()
                .collect(Collectors.toMap(EventDocument::id, document -> document.toDomain(codec), (first, second) -> first));
        return due.stream()
                .map(item -> new PendingDelivery(recorded.get(item.id().eventId()), item.id().consumer(), item.attempts()))
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
        Query ofEvent = Query.query(Criteria.where("id.eventId").is(Objects.requireNonNull(id, "event id").value().toString()))
                .with(Sort.by("id.consumer"));
        return operations.find(ofEvent, DeliveryDocument.class).stream()
                .map(item -> item.progress().toDelivery(item.id().consumer()))
                .toList();
    }

    private void advance(EventId id, String consumer, UnaryOperator<DeliveryProgress> transition) {
        DeliveryDocument.Key key = new DeliveryDocument.Key(id.value().toString(), consumer);
        DeliveryDocument delivery = deliveries.findById(key).orElseThrow(() -> new UnknownResource("delivery", consumer));
        deliveries.save(DeliveryDocument.of(key, delivery.sequence(), transition.apply(delivery.progress())));
    }

    private void createIndexes() {
        var indexes = operations.indexOps(DeliveryDocument.class);
        indexes.createIndex(new Index().on("status", Sort.Direction.ASC).on("nextAttemptAt", Sort.Direction.ASC)
                .named("status_next_attempt"));
        indexes.createIndex(new Index().on("_id.eventId", Sort.Direction.ASC).named("event"));
    }
}
