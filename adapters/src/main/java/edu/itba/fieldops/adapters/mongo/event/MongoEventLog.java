package edu.itba.fieldops.adapters.mongo.event;

import edu.itba.fieldops.adapters.PersistenceProfiles;
import edu.itba.fieldops.adapters.event.EventPayloadCodec;
import edu.itba.fieldops.adapters.mongo.RegistrationSequence;
import edu.itba.fieldops.domain.event.DomainEvent;
import edu.itba.fieldops.domain.event.EventFilter;
import edu.itba.fieldops.domain.event.EventLog;
import edu.itba.fieldops.domain.event.RecordedEvent;
import edu.itba.fieldops.domain.identity.EventId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.shared.Clock;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
@Profile(PersistenceProfiles.MONGO)
@Transactional(readOnly = true)
public class MongoEventLog implements EventLog {
    private static final Sort IN_SEQUENCE = Sort.by("sequence");

    private final EventMongoRepository events;
    private final MongoOperations operations;
    private final RegistrationSequence sequence;
    private final Clock clock;
    private final EventPayloadCodec codec = new EventPayloadCodec();

    MongoEventLog(EventMongoRepository events, MongoOperations operations, RegistrationSequence sequence, Clock clock) {
        this.events = Objects.requireNonNull(events, "events");
        this.operations = Objects.requireNonNull(operations, "operations");
        this.sequence = Objects.requireNonNull(sequence, "sequence");
        this.clock = Objects.requireNonNull(clock, "clock");
        createIndexes();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void append(List<DomainEvent> appended) {
        Instant recordedAt = clock.now();
        events.saveAll(List.copyOf(Objects.requireNonNull(appended, "events")).stream()
                .map(event -> EventDocument.of(
                        UUID.randomUUID(), sequence.next(EventDocument.COLLECTION), event, recordedAt, codec.encode(event)
                ))
                .toList());
    }

    @Override
    public Page<RecordedEvent> page(EventFilter filter, PageRequest request) {
        Objects.requireNonNull(filter, "filter");
        long total = operations.count(matching(filter), EventDocument.class);
        List<RecordedEvent> items = operations.find(
                matching(filter).with(IN_SEQUENCE).skip(request.offset()).limit(request.size()), EventDocument.class
        ).stream().map(document -> document.toDomain(codec)).toList();
        return new Page<>(items, request, total);
    }

    @Override
    public Optional<RecordedEvent> find(EventId id) {
        return events.findById(Objects.requireNonNull(id, "event id").value().toString())
                .map(document -> document.toDomain(codec));
    }

    private static Query matching(EventFilter filter) {
        Query query = new Query();
        if (!filter.expeditions().isEmpty()) {
            query.addCriteria(Criteria.where("expeditionId").in(
                    filter.expeditions().stream().map(ExpeditionId::value).map(UUID::toString).toList()
            ));
        }
        if (!filter.types().isEmpty()) {
            query.addCriteria(Criteria.where("type").in(filter.types()));
        }
        return query;
    }

    private void createIndexes() {
        var indexes = operations.indexOps(EventDocument.class);
        indexes.createIndex(new Index().on("sequence", Sort.Direction.ASC).unique().named("sequence_unique"));
        indexes.createIndex(new Index().on("expeditionId", Sort.Direction.ASC).on("sequence", Sort.Direction.ASC)
                .named("expedition_sequence"));
        indexes.createIndex(new Index().on("type", Sort.Direction.ASC).on("sequence", Sort.Direction.ASC)
                .named("type_sequence"));
    }
}
