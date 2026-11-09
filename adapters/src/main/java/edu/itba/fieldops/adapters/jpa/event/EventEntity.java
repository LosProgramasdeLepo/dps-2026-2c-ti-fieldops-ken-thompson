package edu.itba.fieldops.adapters.jpa.event;

import edu.itba.fieldops.adapters.event.EventPayloadCodec;
import edu.itba.fieldops.domain.event.DomainEvent;
import edu.itba.fieldops.domain.event.RecordedEvent;
import edu.itba.fieldops.domain.identity.EventId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "domain_events")
public class EventEntity implements Persistable<UUID> {
    @Id
    private UUID id;

    @Generated(event = EventType.INSERT)
    @Column(insertable = false, updatable = false)
    private Long sequence;

    @Column(nullable = false, updatable = false)
    private String type;

    @Column(name = "schema_version", nullable = false, updatable = false)
    private int schemaVersion;

    @Column(name = "expedition_id", nullable = false, updatable = false)
    private UUID expeditionId;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false)
    private String payload;

    protected EventEntity() {
    }

    EventEntity(UUID id, DomainEvent event, Instant recordedAt, String payload) {
        this.id = id;
        this.type = event.type();
        this.schemaVersion = EventPayloadCodec.SCHEMA_VERSION;
        this.expeditionId = event.expeditionId().value();
        this.occurredAt = event.occurredAt();
        this.recordedAt = recordedAt;
        this.payload = payload;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    @Transient
    public boolean isNew() {
        return true;
    }

    RecordedEvent toDomain(EventPayloadCodec codec, Map<String, Object> decodedPayload) {
        return new RecordedEvent(new EventId(id), sequence, recordedAt, codec.decode(type, schemaVersion, decodedPayload));
    }

    String payload() {
        return payload;
    }
}
