package edu.itba.fieldops.adapters;

import edu.itba.fieldops.domain.event.EventDeliveries;
import edu.itba.fieldops.domain.event.EventLog;

import java.time.Instant;

class InMemoryEventDeliveriesTest extends EventDeliveriesContract {
    private final InMemoryEventLog log = new InMemoryEventLog(() -> Instant.parse("2026-11-01T08:00:00Z"));
    private final InMemoryEventDeliveries deliveries = new InMemoryEventDeliveries(log);

    @Override
    protected EventDeliveries deliveries() {
        return deliveries;
    }

    @Override
    protected EventLog eventLog() {
        return log;
    }
}
