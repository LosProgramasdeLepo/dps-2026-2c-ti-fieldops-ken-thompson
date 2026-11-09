package edu.itba.fieldops.adapters.mongo.event;

import edu.itba.fieldops.adapters.event.EventPayloadCodec;
import edu.itba.fieldops.domain.event.DomainEvent;
import edu.itba.fieldops.domain.event.RecordedEvent;
import edu.itba.fieldops.domain.identity.EventId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Instants are stored as ISO-8601 strings, not as BSON dates: {@code SystemClock} stamps microseconds and a BSON date
 * keeps milliseconds, so a string is the only way a recorded instant comes back equal. The log is read by
 * {@code sequence}, never by time, so nothing needs the date type.
 */
@Document(collection = EventDocument.COLLECTION)
record EventDocument(
        @Id String id,
        long sequence,
        String type,
        int schemaVersion,
        String expeditionId,
        String occurredAt,
        String recordedAt,
        Map<String, Object> payload
) {
    static final String COLLECTION = "domain_events";

    static EventDocument of(UUID id, long sequence, DomainEvent event, Instant recordedAt, Map<String, Object> payload) {
        return new EventDocument(
                id.toString(),
                sequence,
                event.type(),
                EventPayloadCodec.SCHEMA_VERSION,
                event.expeditionId().value().toString(),
                event.occurredAt().toString(),
                recordedAt.toString(),
                payload
        );
    }

    RecordedEvent toDomain(EventPayloadCodec codec) {
        return new RecordedEvent(
                new EventId(UUID.fromString(id)),
                sequence,
                Instant.parse(recordedAt),
                codec.decode(type, schemaVersion, payload)
        );
    }
}
