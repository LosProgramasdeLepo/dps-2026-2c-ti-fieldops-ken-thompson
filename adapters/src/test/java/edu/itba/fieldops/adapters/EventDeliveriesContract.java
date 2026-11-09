package edu.itba.fieldops.adapters;

import edu.itba.fieldops.domain.event.Delivery;
import edu.itba.fieldops.domain.event.DeliveryFailure;
import edu.itba.fieldops.domain.event.DeliveryStatus;
import edu.itba.fieldops.domain.event.EventDeliveries;
import edu.itba.fieldops.domain.event.EventFilter;
import edu.itba.fieldops.domain.event.EventLog;
import edu.itba.fieldops.domain.event.PendingDelivery;
import edu.itba.fieldops.domain.event.RecordedEvent;
import edu.itba.fieldops.domain.event.RunStarted;
import edu.itba.fieldops.domain.identity.EventId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.shared.PageRequest;
import edu.itba.fieldops.domain.shared.UnknownResource;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class EventDeliveriesContract {
    private static final int EVERYTHING = 100_000;
    private static final Instant AT = Instant.parse("2026-11-01T08:00:00Z");
    private static final Instant RETRY_AT = AT.plus(Duration.ofMinutes(1));

    protected abstract EventDeliveries deliveries();

    protected abstract EventLog eventLog();

    protected void inTransaction(Runnable work) {
        work.run();
    }

    private EventId recorded() {
        ExpeditionId expeditionId = new ExpeditionId(UUID.randomUUID());
        inTransaction(() -> eventLog().append(List.of(new RunStarted(expeditionId, AT))));
        return eventLog().page(EventFilter.forExpeditions(Set.of(expeditionId)), new PageRequest(0, 1)).items().getFirst().id();
    }

    private void schedule(EventId id, String... consumers) {
        inTransaction(() -> deliveries().schedule(id, Set.of(consumers), AT));
    }

    private void succeed(EventId id, String consumer, Instant at) {
        inTransaction(() -> deliveries().succeeded(id, consumer, at));
    }

    private void fail(EventId id, String consumer, DeliveryFailure failure) {
        inTransaction(() -> deliveries().failed(id, consumer, failure));
    }

    private void retry(EventId id, String consumer, Instant at) {
        inTransaction(() -> deliveries().retry(id, consumer, at));
    }

    private List<EventId> undispatched() {
        return deliveries().undispatched(EVERYTHING).stream().map(RecordedEvent::id).toList();
    }

    private List<String> dueOf(EventId id, Instant now) {
        return deliveries().due(now, EVERYTHING).stream()
                .filter(item -> item.event().id().equals(id))
                .map(PendingDelivery::consumer)
                .toList();
    }

    private Delivery only(EventId id) {
        List<Delivery> found = deliveries().of(id);
        assertEquals(1, found.size());
        return found.getFirst();
    }

    private static DeliveryFailure retriable(String error) {
        return new DeliveryFailure(AT, error, Optional.of(RETRY_AT));
    }

    private static DeliveryFailure terminal(String error) {
        return new DeliveryFailure(AT, error, Optional.empty());
    }

    @Test
    void aNewEventIsUndispatchedInSequenceOrder() {
        EventId first = recorded();
        EventId second = recorded();

        List<EventId> pending = undispatched();

        assertTrue(pending.indexOf(first) >= 0);
        assertTrue(pending.indexOf(first) < pending.indexOf(second));
    }

    @Test
    void undispatchedHonoursTheLimit() {
        recorded();
        recorded();

        assertEquals(1, deliveries().undispatched(1).size());
    }

    @Test
    void schedulingCreatesAPendingDeliveryPerConsumerSortedByName() {
        EventId id = recorded();

        schedule(id, "zeta", "alpha");

        assertAll(
                () -> assertFalse(undispatched().contains(id)),
                () -> assertEquals(
                        List.of(
                                new Delivery("alpha", DeliveryStatus.PENDING, 0, Optional.empty(), Optional.empty()),
                                new Delivery("zeta", DeliveryStatus.PENDING, 0, Optional.empty(), Optional.empty())
                        ),
                        deliveries().of(id)
                )
        );
    }

    @Test
    void anEventWithoutSubscribersStopsBeingUndispatched() {
        EventId id = recorded();

        schedule(id);

        assertAll(
                () -> assertFalse(undispatched().contains(id)),
                () -> assertEquals(List.of(), deliveries().of(id))
        );
    }

    @Test
    void schedulingAgainChangesNothing() {
        EventId id = recorded();
        schedule(id, "alpha");
        succeed(id, "alpha", AT);

        schedule(id, "alpha", "beta");

        assertEquals(DeliveryStatus.DELIVERED, only(id).status());
    }

    @Test
    void aScheduledDeliveryIsDueFromItsInstant() {
        EventId id = recorded();
        schedule(id, "alpha");

        assertAll(
                () -> assertEquals(List.of(), dueOf(id, AT.minusSeconds(1))),
                () -> assertEquals(List.of("alpha"), dueOf(id, AT))
        );
    }

    @Test
    void dueCarriesTheEventAndTheAttemptsSoFar() {
        EventId id = recorded();
        schedule(id, "alpha");
        fail(id, "alpha", retriable("boom"));

        PendingDelivery pending = deliveries().due(RETRY_AT, EVERYTHING).stream()
                .filter(item -> item.event().id().equals(id))
                .findFirst()
                .orElseThrow();

        assertAll(
                () -> assertEquals("alpha", pending.consumer()),
                () -> assertEquals(1, pending.attempts()),
                () -> assertEquals(id, pending.event().id())
        );
    }

    @Test
    void dueIsOrderedBySequenceThenConsumer() {
        EventId first = recorded();
        EventId second = recorded();
        schedule(second, "beta", "alpha");
        schedule(first, "gamma");

        List<String> order = deliveries().due(AT, EVERYTHING).stream()
                .filter(item -> Set.of(first, second).contains(item.event().id()))
                .map(item -> item.consumer())
                .toList();

        assertEquals(List.of("gamma", "alpha", "beta"), order);
    }

    @Test
    void dueHonoursTheLimit() {
        EventId id = recorded();
        schedule(id, "alpha", "beta");

        assertEquals(1, deliveries().due(AT, 1).size());
    }

    @Test
    void aSuccessLeavesTheDeliveryDelivered() {
        EventId id = recorded();
        schedule(id, "alpha");

        succeed(id, "alpha", RETRY_AT);

        assertAll(
                () -> assertEquals(new Delivery("alpha", DeliveryStatus.DELIVERED, 1, Optional.of(RETRY_AT), Optional.empty()), only(id)),
                () -> assertEquals(List.of(), dueOf(id, RETRY_AT.plusSeconds(3600)))
        );
    }

    @Test
    void aFailureWithARetryLeavesTheDeliveryRetrying() {
        EventId id = recorded();
        schedule(id, "alpha");

        fail(id, "alpha", retriable("boom"));

        assertAll(
                () -> assertEquals(new Delivery("alpha", DeliveryStatus.RETRYING, 1, Optional.of(AT), Optional.of("boom")), only(id)),
                () -> assertEquals(List.of(), dueOf(id, RETRY_AT.minusSeconds(1))),
                () -> assertEquals(List.of("alpha"), dueOf(id, RETRY_AT))
        );
    }

    @Test
    void aFailureWithoutARetryLeavesTheDeliveryFailedAndNotDue() {
        EventId id = recorded();
        schedule(id, "alpha");
        fail(id, "alpha", retriable("boom"));

        fail(id, "alpha", terminal("gave up"));

        assertAll(
                () -> assertEquals(new Delivery("alpha", DeliveryStatus.FAILED, 2, Optional.of(AT), Optional.of("gave up")), only(id)),
                () -> assertEquals(List.of(), dueOf(id, RETRY_AT.plusSeconds(3600)))
        );
    }

    @Test
    void aPendingDeliveryCanFailDirectlyWithASingleAttempt() {
        EventId id = recorded();
        schedule(id, "alpha");

        fail(id, "alpha", terminal("boom"));

        assertEquals(DeliveryStatus.FAILED, only(id).status());
    }

    @Test
    void aManualRetryResetsTheAttemptsAndKeepsTheLastError() {
        EventId id = recorded();
        schedule(id, "alpha");
        fail(id, "alpha", terminal("boom"));

        retry(id, "alpha", RETRY_AT);

        assertAll(
                () -> assertEquals(new Delivery("alpha", DeliveryStatus.PENDING, 0, Optional.of(AT), Optional.of("boom")), only(id)),
                () -> assertEquals(List.of(), dueOf(id, RETRY_AT.minusSeconds(1))),
                () -> assertEquals(List.of("alpha"), dueOf(id, RETRY_AT))
        );
    }

    @Test
    void aSuccessAfterAFailureKeepsTheLastError() {
        EventId id = recorded();
        schedule(id, "alpha");
        fail(id, "alpha", retriable("boom"));

        succeed(id, "alpha", RETRY_AT);

        assertEquals(new Delivery("alpha", DeliveryStatus.DELIVERED, 2, Optional.of(RETRY_AT), Optional.of("boom")), only(id));
    }

    @Test
    void anEventWithoutDeliveriesHasNone() {
        assertEquals(List.of(), deliveries().of(new EventId(UUID.randomUUID())));
    }

    @Test
    void anUnknownDeliveryCannotAdvance() {
        EventId id = recorded();

        assertAll(
                () -> assertThrows(UnknownResource.class, () -> succeed(id, "ghost", AT)),
                () -> assertThrows(UnknownResource.class, () -> fail(id, "ghost", terminal("boom"))),
                () -> assertThrows(UnknownResource.class, () -> retry(id, "ghost", AT))
        );
    }
}
