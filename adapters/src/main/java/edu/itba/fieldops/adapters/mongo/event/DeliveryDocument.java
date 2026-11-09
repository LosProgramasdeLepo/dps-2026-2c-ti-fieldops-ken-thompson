package edu.itba.fieldops.adapters.mongo.event;

import edu.itba.fieldops.adapters.event.DeliveryProgress;
import edu.itba.fieldops.domain.event.DeliveryStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Optional;

@Document(collection = DeliveryDocument.COLLECTION)
record DeliveryDocument(
        @Id Key id,
        long sequence,
        String status,
        int attempts,
        Instant nextAttemptAt,
        Instant lastAttemptAt,
        String lastError,
        Instant deliveredAt
) {
    static final String COLLECTION = "event_deliveries";

    static DeliveryDocument of(Key id, long sequence, DeliveryProgress progress) {
        return new DeliveryDocument(
                id,
                sequence,
                progress.status().name(),
                progress.attempts(),
                progress.nextAttemptAt().orElse(null),
                progress.lastAttemptAt().orElse(null),
                progress.lastError().orElse(null),
                progress.deliveredAt().orElse(null)
        );
    }

    DeliveryProgress progress() {
        return new DeliveryProgress(
                DeliveryStatus.valueOf(status),
                attempts,
                Optional.ofNullable(nextAttemptAt),
                Optional.ofNullable(lastAttemptAt),
                Optional.ofNullable(lastError),
                Optional.ofNullable(deliveredAt)
        );
    }

    record Key(String eventId, String consumer) {
    }
}
