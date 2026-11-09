package edu.itba.fieldops.api.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class EventResponses {
    private EventResponses() {
    }

    public record EventResponse(
            UUID id,
            long sequence,
            String type,
            UUID expeditionId,
            Instant occurredAt,
            Instant recordedAt,
            Map<String, Object> payload
    ) {
    }
}
