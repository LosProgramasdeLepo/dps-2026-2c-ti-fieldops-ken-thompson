package edu.itba.fieldops.adapters.messaging;

import edu.itba.fieldops.domain.event.DeliveryFailure;
import edu.itba.fieldops.domain.event.EventDeliveries;
import edu.itba.fieldops.domain.event.PendingDelivery;
import edu.itba.fieldops.domain.event.RecordedEvent;
import edu.itba.fieldops.domain.shared.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class EventDispatcher {
    private static final Logger log = LoggerFactory.getLogger(EventDispatcher.class);
    private static final int MAX_ERROR_LENGTH = 500;

    private final EventDeliveries deliveries;
    private final Map<String, EventConsumer> consumers;
    private final DeliveryTransaction transaction;
    private final Clock clock;
    private final int batchSize;
    private final RetryPolicy retries;

    public EventDispatcher(
            EventDeliveries deliveries,
            List<EventConsumer> consumers,
            DeliveryTransaction transaction,
            Clock clock,
            EventsProperties properties
    ) {
        this.deliveries = Objects.requireNonNull(deliveries, "deliveries");
        this.consumers = consumers.stream().collect(Collectors.toUnmodifiableMap(EventConsumer::name, Function.identity()));
        this.transaction = Objects.requireNonNull(transaction, "transaction");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.batchSize = properties.batchSize();
        this.retries = properties.retryPolicy();
    }

    public void dispatchDue() {
        deliveries.undispatched(batchSize).forEach(this::fanOut);
        deliveries.due(clock.now(), batchSize).forEach(this::deliver);
    }

    private void fanOut(RecordedEvent recorded) {
        Set<String> subscribers = consumers.values().stream()
                .filter(consumer -> consumer.subscribesTo(recorded.event()))
                .map(EventConsumer::name)
                .collect(Collectors.toSet());
        deliveries.schedule(recorded.id(), subscribers, clock.now());
    }

    private void deliver(PendingDelivery pending) {
        try {
            transaction.run(() -> {
                consumerOf(pending).consume(pending.event());
                deliveries.succeeded(pending.event().id(), pending.consumer(), clock.now());
            });
        } catch (RuntimeException failure) {
            recordFailure(pending, failure);
        }
    }

    private EventConsumer consumerOf(PendingDelivery pending) {
        return Optional.ofNullable(consumers.get(pending.consumer()))
                .orElseThrow(() -> new IllegalStateException("no consumer named " + pending.consumer()));
    }

    private void recordFailure(PendingDelivery pending, RuntimeException failure) {
        log.warn("Delivery of event {} to {} failed", pending.event().id().value(), pending.consumer(), failure);
        Instant at = clock.now();
        DeliveryFailure outcome = new DeliveryFailure(
                at, describe(failure), retries.nextAttemptAfter(pending.attempts() + 1, at)
        );
        try {
            deliveries.failed(pending.event().id(), pending.consumer(), outcome);
        } catch (RuntimeException unrecorded) {
            log.error("Could not record the failed delivery of event {} to {}", pending.event().id().value(), pending.consumer(), unrecorded);
        }
    }

    private static String describe(RuntimeException failure) {
        String type = failure.getClass().getName();
        String description = failure.getMessage() == null ? type : type + ": " + failure.getMessage();
        return description.codePointCount(0, description.length()) <= MAX_ERROR_LENGTH
                ? description
                : description.substring(0, description.offsetByCodePoints(0, MAX_ERROR_LENGTH));
    }
}
