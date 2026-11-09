package edu.itba.fieldops.adapters.messaging;

import edu.itba.fieldops.adapters.InMemoryEventDeliveries;
import edu.itba.fieldops.adapters.InMemoryEventLog;
import edu.itba.fieldops.domain.event.Delivery;
import edu.itba.fieldops.domain.event.DeliveryFailure;
import edu.itba.fieldops.domain.event.DeliveryStatus;
import edu.itba.fieldops.domain.event.DomainEvent;
import edu.itba.fieldops.domain.event.EventDeliveries;
import edu.itba.fieldops.domain.event.EventFilter;
import edu.itba.fieldops.domain.event.PendingDelivery;
import edu.itba.fieldops.domain.event.RecordedEvent;
import edu.itba.fieldops.domain.event.RunFinished;
import edu.itba.fieldops.domain.event.RunStarted;
import edu.itba.fieldops.domain.identity.EventId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.shared.Clock;
import edu.itba.fieldops.domain.shared.PageRequest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventDispatcherTest {
    private static final Instant T0 = Instant.parse("2026-11-01T08:00:00Z");
    private static final Duration BASE = Duration.ofSeconds(1);
    private static final EventsProperties PROPERTIES = new EventsProperties(50, 3, BASE, Duration.ofMinutes(1));

    private final MovableClock clock = new MovableClock();
    private final InMemoryEventLog log = new InMemoryEventLog(clock);
    private final InMemoryEventDeliveries deliveries = new InMemoryEventDeliveries(log);

    private EventDispatcher dispatcher(EventConsumer... consumers) {
        return new EventDispatcher(deliveries, List.of(consumers), Runnable::run, clock, PROPERTIES);
    }

    private EventId recorded() {
        ExpeditionId expeditionId = new ExpeditionId(UUID.randomUUID());
        log.append(List.of(new RunStarted(expeditionId, T0)));
        return log.page(EventFilter.forExpeditions(Set.of(expeditionId)), new PageRequest(0, 1)).items().getFirst().id();
    }

    private Delivery only(EventId id) {
        List<Delivery> found = deliveries.of(id);
        assertEquals(1, found.size());
        return found.getFirst();
    }

    @Test
    void fansOutOnlyToSubscribedConsumers() {
        EventId id = recorded();

        dispatcher(new Recording("yes"), new Recording("no", event -> false)).dispatchDue();

        assertEquals(List.of("yes"), deliveries.of(id).stream().map(Delivery::consumer).toList());
    }

    @Test
    void anEventWithoutSubscribersIsNotOfferedAgain() {
        recorded();

        dispatcher(new Recording("no", event -> false)).dispatchDue();

        assertEquals(List.of(), deliveries.undispatched(10));
    }

    @Test
    void aSuccessLeavesTheDeliveryDelivered() {
        EventId id = recorded();
        Recording consumer = new Recording("a");

        dispatcher(consumer).dispatchDue();

        assertAll(
                () -> assertEquals(List.of(id), consumer.seen),
                () -> assertEquals(new Delivery("a", DeliveryStatus.DELIVERED, 1, Optional.of(T0), Optional.empty()), only(id))
        );
    }

    @Test
    void aDeliveredEventIsNotDeliveredAgain() {
        recorded();
        Recording consumer = new Recording("a");
        EventDispatcher dispatcher = dispatcher(consumer);

        dispatcher.dispatchDue();
        clock.advance(Duration.ofHours(1));
        dispatcher.dispatchDue();

        assertEquals(1, consumer.seen.size());
    }

    @Test
    void anExceptionLeavesTheDeliveryRetryingAfterTheBaseWait() {
        EventId id = recorded();

        dispatcher(new Recording("a").failing(1)).dispatchDue();

        assertAll(
                () -> assertEquals(
                        new Delivery("a", DeliveryStatus.RETRYING, 1, Optional.of(T0), Optional.of("java.lang.IllegalStateException: boom")),
                        only(id)
                ),
                () -> assertEquals(List.of(), deliveries.due(T0.plus(BASE).minusNanos(1), 10)),
                () -> assertEquals(1, deliveries.due(T0.plus(BASE), 10).size())
        );
    }

    @Test
    void doesNotRetryBeforeTheWaitAndRetriesAfterIt() {
        recorded();
        Recording consumer = new Recording("a").failing(1);
        EventDispatcher dispatcher = dispatcher(consumer);
        dispatcher.dispatchDue();

        clock.advance(BASE.minusMillis(1));
        dispatcher.dispatchDue();
        int beforeTheWait = consumer.attempts;
        clock.advance(Duration.ofMillis(1));
        dispatcher.dispatchDue();

        assertAll(
                () -> assertEquals(1, beforeTheWait),
                () -> assertEquals(2, consumer.attempts)
        );
    }

    @Test
    void aSuccessOnTheSecondAttemptLeavesTheDeliveryDelivered() {
        EventId id = recorded();
        EventDispatcher dispatcher = dispatcher(new Recording("a").failing(1));
        dispatcher.dispatchDue();

        clock.advance(BASE);
        dispatcher.dispatchDue();

        assertAll(
                () -> assertEquals(DeliveryStatus.DELIVERED, only(id).status()),
                () -> assertEquals(2, only(id).attempts())
        );
    }

    @Test
    void aPermanentFailureLeavesTheDeliveryFailedWithoutMoreAttempts() {
        EventId id = recorded();
        Recording consumer = new Recording("a").failing(Integer.MAX_VALUE);
        EventDispatcher dispatcher = dispatcher(consumer);

        for (int pass = 0; pass < 10; pass++) {
            dispatcher.dispatchDue();
            clock.advance(Duration.ofHours(1));
        }

        assertAll(
                () -> assertEquals(DeliveryStatus.FAILED, only(id).status()),
                () -> assertEquals(3, only(id).attempts()),
                () -> assertEquals(3, consumer.attempts)
        );
    }

    @Test
    void theWaitGrowsBetweenAttempts() {
        recorded();
        Recording consumer = new Recording("a").failing(Integer.MAX_VALUE);
        EventDispatcher dispatcher = dispatcher(consumer);
        dispatcher.dispatchDue();
        clock.advance(BASE);
        dispatcher.dispatchDue();

        clock.advance(BASE);
        dispatcher.dispatchDue();
        int beforeTheSecondWait = consumer.attempts;
        clock.advance(BASE);
        dispatcher.dispatchDue();

        assertAll(
                () -> assertEquals(2, beforeTheSecondWait),
                () -> assertEquals(3, consumer.attempts)
        );
    }

    @Test
    void aSingleAttemptGoesFromPendingToFailed() {
        EventId id = recorded();
        EventsProperties once = new EventsProperties(50, 1, BASE, Duration.ofMinutes(1));

        new EventDispatcher(deliveries, List.of(new Recording("a").failing(1)), Runnable::run, clock, once).dispatchDue();

        assertEquals(DeliveryStatus.FAILED, only(id).status());
    }

    @Test
    void theFailureOfOneConsumerDoesNotAffectAnother() {
        EventId id = recorded();
        Recording healthy = new Recording("b");

        dispatcher(new Recording("a").failing(Integer.MAX_VALUE), healthy).dispatchDue();

        assertAll(
                () -> assertEquals(List.of(id), healthy.seen),
                () -> assertEquals(
                        List.of(DeliveryStatus.RETRYING, DeliveryStatus.DELIVERED),
                        deliveries.of(id).stream().map(Delivery::status).toList()
                )
        );
    }

    @Test
    void aLaterEventIsProcessedWhileAnEarlierOneIsRetrying() {
        EventId earlier = recorded();
        EventId later = recorded();

        dispatcher(new Recording("a").failingOn(earlier)).dispatchDue();

        assertAll(
                () -> assertEquals(DeliveryStatus.RETRYING, only(earlier).status()),
                () -> assertEquals(DeliveryStatus.DELIVERED, only(later).status())
        );
    }

    @Test
    void firstAttemptsFollowTheSequence() {
        EventId first = recorded();
        EventId second = recorded();
        EventId third = recorded();
        Recording consumer = new Recording("a");

        dispatcher(consumer).dispatchDue();

        assertEquals(List.of(first, second, third), consumer.seen);
    }

    @Test
    void honoursTheBatchSize() {
        recorded();
        recorded();
        Recording consumer = new Recording("a");
        EventsProperties one = new EventsProperties(1, 3, BASE, Duration.ofMinutes(1));
        EventDispatcher dispatcher = new EventDispatcher(deliveries, List.of(consumer), Runnable::run, clock, one);

        dispatcher.dispatchDue();
        int afterTheFirstPass = consumer.attempts;
        dispatcher.dispatchDue();

        assertAll(
                () -> assertEquals(1, afterTheFirstPass),
                () -> assertEquals(2, consumer.attempts)
        );
    }

    @Test
    void theConsumerAndTheSuccessRunInsideTheSameTransaction() {
        EventId id = recorded();
        AtomicBoolean inside = new AtomicBoolean();
        List<Boolean> consumedInside = new ArrayList<>();
        DeliveryTransaction transaction = work -> {
            inside.set(true);
            work.run();
            consumedInside.add(deliveries.of(id).getFirst().status() == DeliveryStatus.DELIVERED);
            inside.set(false);
        };
        Recording consumer = new Recording("a") {
            @Override
            public void consume(RecordedEvent event) {
                consumedInside.add(inside.get());
                super.consume(event);
            }
        };

        new EventDispatcher(deliveries, List.of(consumer), transaction, clock, PROPERTIES).dispatchDue();

        assertEquals(List.of(true, true), consumedInside);
    }

    @Test
    void aFailureDuringTheTransactionIsRecordedOutsideIt() {
        EventId id = recorded();
        DeliveryTransaction failingCommit = work -> {
            work.run();
            throw new IllegalStateException("commit failed");
        };

        EventDeliveries forgetful = new ForgetsSuccess(deliveries);

        new EventDispatcher(forgetful, List.of(new Recording("a")), failingCommit, clock, PROPERTIES).dispatchDue();

        assertAll(
                () -> assertEquals(DeliveryStatus.RETRYING, only(id).status()),
                () -> assertEquals(Optional.of("java.lang.IllegalStateException: commit failed"), only(id).lastError())
        );
    }

    @Test
    void aFailureThatCannotBeRecordedDoesNotStopThePassAndStaysDue() {
        EventId broken = recorded();
        EventId healthy = recorded();
        EventDeliveries cannotRecord = new CannotRecordFailures(deliveries);

        new EventDispatcher(cannotRecord, List.of(new Recording("a").failingOn(broken)), Runnable::run, clock, PROPERTIES).dispatchDue();

        assertAll(
                () -> assertEquals(DeliveryStatus.DELIVERED, only(healthy).status()),
                () -> assertEquals(DeliveryStatus.PENDING, only(broken).status()),
                () -> assertEquals(1, deliveries.due(T0, 10).size())
        );
    }

    @Test
    void theErrorHasClassAndMessageButNoStackTrace() {
        EventId id = recorded();

        dispatcher(new Recording("a").throwing(new IllegalArgumentException("bad input"))).dispatchDue();

        String error = only(id).lastError().orElseThrow();
        assertAll(
                () -> assertEquals("java.lang.IllegalArgumentException: bad input", error),
                () -> assertFalse(error.contains("\tat "))
        );
    }

    @Test
    void anErrorWithoutMessageKeepsTheClass() {
        EventId id = recorded();

        dispatcher(new Recording("a").throwing(new IllegalStateException())).dispatchDue();

        assertEquals(Optional.of("java.lang.IllegalStateException"), only(id).lastError());
    }

    @Test
    void aLongErrorIsTruncated() {
        EventId id = recorded();

        dispatcher(new Recording("a").throwing(new IllegalStateException("x".repeat(10_000)))).dispatchDue();

        assertEquals(500, only(id).lastError().orElseThrow().length());
    }

    @Test
    void truncatingDoesNotSplitACharacter() {
        EventId id = recorded();

        dispatcher(new Recording("a").throwing(new IllegalStateException("😀".repeat(1_000)))).dispatchDue();

        String error = only(id).lastError().orElseThrow();
        assertAll(
                () -> assertEquals(500, error.codePointCount(0, error.length())),
                () -> assertTrue(Character.isHighSurrogate(error.charAt(error.length() - 2)))
        );
    }

    @Test
    void aDeliveryWhoseConsumerNoLongerExistsFails() {
        EventId id = recorded();
        deliveries.schedule(id, Set.of("retired"), T0);

        dispatcher(new Recording("a")).dispatchDue();

        assertAll(
                () -> assertEquals(DeliveryStatus.RETRYING, only(id).status()),
                () -> assertEquals(Optional.of("java.lang.IllegalStateException: no consumer named retired"), only(id).lastError())
        );
    }

    private final class MovableClock implements Clock {
        private Instant now = T0;

        @Override
        public Instant now() {
            return now;
        }

        void advance(Duration elapsed) {
            now = now.plus(elapsed);
        }
    }

    private static class Recording implements EventConsumer {
        final List<EventId> seen = new ArrayList<>();
        int attempts;
        private final String name;
        private final Predicate<DomainEvent> subscribed;
        private int failuresLeft;
        private RuntimeException failure = new IllegalStateException("boom");
        private Optional<EventId> failingEvent = Optional.empty();

        Recording(String name) {
            this(name, event -> event instanceof RunStarted || event instanceof RunFinished);
        }

        Recording(String name, Predicate<DomainEvent> subscribed) {
            this.name = name;
            this.subscribed = subscribed;
        }

        Recording failing(int times) {
            failuresLeft = times;
            return this;
        }

        Recording throwing(RuntimeException error) {
            failure = error;
            failuresLeft = Integer.MAX_VALUE;
            return this;
        }

        Recording failingOn(EventId event) {
            failingEvent = Optional.of(event);
            return this;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public boolean subscribesTo(DomainEvent event) {
            return subscribed.test(event);
        }

        @Override
        public void consume(RecordedEvent event) {
            attempts++;
            if (failuresLeft > 0 || failingEvent.filter(event.id()::equals).isPresent()) {
                failuresLeft--;
                throw failure;
            }
            seen.add(event.id());
        }
    }

    private abstract static class Delegating implements EventDeliveries {
        private final EventDeliveries delegate;

        Delegating(EventDeliveries delegate) {
            this.delegate = delegate;
        }

        @Override
        public List<RecordedEvent> undispatched(int limit) {
            return delegate.undispatched(limit);
        }

        @Override
        public void schedule(EventId id, Set<String> consumers, Instant at) {
            delegate.schedule(id, consumers, at);
        }

        @Override
        public List<PendingDelivery> due(Instant now, int limit) {
            return delegate.due(now, limit);
        }

        @Override
        public void succeeded(EventId id, String consumer, Instant at) {
            delegate.succeeded(id, consumer, at);
        }

        @Override
        public void failed(EventId id, String consumer, DeliveryFailure failure) {
            delegate.failed(id, consumer, failure);
        }

        @Override
        public void retry(EventId id, String consumer, Instant at) {
            delegate.retry(id, consumer, at);
        }

        @Override
        public List<Delivery> of(EventId id) {
            return delegate.of(id);
        }
    }

    private static final class CannotRecordFailures extends Delegating {
        CannotRecordFailures(EventDeliveries delegate) {
            super(delegate);
        }

        @Override
        public void failed(EventId id, String consumer, DeliveryFailure failure) {
            throw new IllegalStateException("store is down");
        }
    }

    private static final class ForgetsSuccess extends Delegating {
        ForgetsSuccess(EventDeliveries delegate) {
            super(delegate);
        }

        @Override
        public void succeeded(EventId id, String consumer, Instant at) {
        }
    }
}
