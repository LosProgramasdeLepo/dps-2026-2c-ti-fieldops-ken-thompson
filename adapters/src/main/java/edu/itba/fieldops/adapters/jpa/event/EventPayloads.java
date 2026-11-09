package edu.itba.fieldops.adapters.jpa.event;

import edu.itba.fieldops.adapters.event.EventPayloadCodec;
import edu.itba.fieldops.domain.event.DomainEvent;
import edu.itba.fieldops.domain.event.RecordedEvent;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

final class EventPayloads {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final TypeReference<Map<String, Object>> OBJECT = new TypeReference<>() {
    };
    private static final EventPayloadCodec CODEC = new EventPayloadCodec();

    private EventPayloads() {
    }

    static String encode(DomainEvent event) {
        return JSON.writeValueAsString(CODEC.encode(event));
    }

    static RecordedEvent decode(EventEntity entity) {
        return entity.toDomain(CODEC, JSON.readValue(entity.payload(), OBJECT));
    }
}
