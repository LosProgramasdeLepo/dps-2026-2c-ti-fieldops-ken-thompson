package edu.itba.fieldops.adapters.jpa.event;

import edu.itba.fieldops.adapters.EventDeliveriesContract;
import edu.itba.fieldops.adapters.PersistenceProfiles;
import edu.itba.fieldops.adapters.jpa.JpaRepositoryTest;
import edu.itba.fieldops.domain.event.EventDeliveries;
import edu.itba.fieldops.domain.event.EventLog;
import edu.itba.fieldops.domain.identity.EventId;
import edu.itba.fieldops.domain.shared.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@JpaRepositoryTest
@ActiveProfiles(PersistenceProfiles.SQL)
@Import({JpaEventLog.class, JpaEventDeliveries.class, EventDeliveriesRepositoryTest.RecordingTime.class})
class EventDeliveriesRepositoryTest extends EventDeliveriesContract {
    private static final Instant AT = Instant.parse("2026-11-01T08:00:00Z");

    @Autowired
    private EventDeliveries deliveries;
    @Autowired
    private EventLog log;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager transactions;

    @Override
    protected void inTransaction(Runnable work) {
        new TransactionTemplate(transactions).executeWithoutResult(status -> work.run());
    }

    @Override
    protected EventDeliveries deliveries() {
        return deliveries;
    }

    @Override
    protected EventLog eventLog() {
        return log;
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void succeededRequiresAnOpenTransaction() {
        EventId id = new EventId(UUID.randomUUID());

        assertThrows(IllegalTransactionStateException.class, () -> deliveries.succeeded(id, "alpha", AT));
    }

    @Test
    void aDeliveredRowMustKnowWhen() {
        UUID event = insertedEvent();

        assertRejected("""
                INSERT INTO event_deliveries (event_id, consumer, status, attempts)
                VALUES ('%s', 'alpha', 'DELIVERED', 1)
                """.formatted(event));
    }

    @Test
    void aRowCannotClaimItWasDeliveredWhileNotDelivered() {
        UUID event = insertedEvent();

        assertRejected("""
                INSERT INTO event_deliveries (event_id, consumer, status, attempts, next_attempt_at, delivered_at)
                VALUES ('%s', 'alpha', 'PENDING', 0, now(), now())
                """.formatted(event));
    }

    @Test
    void aPendingRowNeedsItsNextAttempt() {
        UUID event = insertedEvent();

        assertRejected("""
                INSERT INTO event_deliveries (event_id, consumer, status, attempts)
                VALUES ('%s', 'alpha', 'RETRYING', 1)
                """.formatted(event));
    }

    @Test
    void aFinishedRowHasNoNextAttempt() {
        UUID event = insertedEvent();

        assertRejected("""
                INSERT INTO event_deliveries (event_id, consumer, status, attempts, next_attempt_at)
                VALUES ('%s', 'alpha', 'FAILED', 1, now())
                """.formatted(event));
    }

    @Test
    void anUnknownStatusIsRejected() {
        UUID event = insertedEvent();

        assertRejected("""
                INSERT INTO event_deliveries (event_id, consumer, status, attempts, next_attempt_at)
                VALUES ('%s', 'alpha', 'LOST', 0, now())
                """.formatted(event));
    }

    @Test
    void negativeAttemptsAreRejected() {
        UUID event = insertedEvent();

        assertRejected("""
                INSERT INTO event_deliveries (event_id, consumer, status, attempts, next_attempt_at)
                VALUES ('%s', 'alpha', 'PENDING', -1, now())
                """.formatted(event));
    }

    @Test
    void aDeliveryNeedsAnExistingEvent() {
        assertRejected("""
                INSERT INTO event_deliveries (event_id, consumer, status, attempts, next_attempt_at)
                VALUES ('%s', 'alpha', 'PENDING', 0, now())
                """.formatted(UUID.randomUUID()));
    }

    @Test
    void aFanoutNeedsAnExistingEvent() {
        assertRejected("INSERT INTO event_fanout (event_id, dispatched_at) VALUES ('%s', now())".formatted(UUID.randomUUID()));
    }

    private UUID insertedEvent() {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO domain_events (id, type, schema_version, expedition_id, occurred_at, recorded_at, payload)
                VALUES (?, 'RunStarted', 1, ?, now(), now(), '{}'::jsonb)
                """, id, UUID.randomUUID());
        return id;
    }

    private void assertRejected(String sql) {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(sql));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RecordingTime {
        @Bean
        Clock clock() {
            return () -> AT;
        }
    }
}
