package edu.itba.fieldops.domain.event;

import edu.itba.fieldops.domain.identity.EventId;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public interface EventDeliveries extends Deliveries {
    List<RecordedEvent> undispatched(int limit);

    void schedule(EventId id, Set<String> consumers, Instant at);

    List<PendingDelivery> due(Instant now, int limit);

    void succeeded(EventId id, String consumer, Instant at);

    void failed(EventId id, String consumer, DeliveryFailure failure);

    void retry(EventId id, String consumer, Instant at);
}
