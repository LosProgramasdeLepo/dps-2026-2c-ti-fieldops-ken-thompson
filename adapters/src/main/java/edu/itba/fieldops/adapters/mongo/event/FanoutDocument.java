package edu.itba.fieldops.adapters.mongo.event;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = FanoutDocument.COLLECTION)
record FanoutDocument(@Id String eventId, Instant dispatchedAt) {
    static final String COLLECTION = "event_fanout";
}
