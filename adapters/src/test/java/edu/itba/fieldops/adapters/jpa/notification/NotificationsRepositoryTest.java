package edu.itba.fieldops.adapters.jpa.notification;

import edu.itba.fieldops.adapters.NotificationInboxContract;
import edu.itba.fieldops.adapters.PersistenceProfiles;
import edu.itba.fieldops.adapters.jpa.JpaRepositoryTest;
import edu.itba.fieldops.adapters.jpa.catalog.JpaPersonRegistry;
import edu.itba.fieldops.adapters.jpa.event.JpaEventLog;
import edu.itba.fieldops.adapters.notification.NotificationInbox;
import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.catalog.PersonName;
import edu.itba.fieldops.domain.catalog.PersonRegistry;
import edu.itba.fieldops.domain.event.EventLog;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.shared.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@JpaRepositoryTest
@ActiveProfiles(PersistenceProfiles.SQL)
@Import({JpaEventLog.class, JpaNotificationInbox.class, JpaPersonRegistry.class, NotificationsRepositoryTest.RecordingTime.class})
class NotificationsRepositoryTest extends NotificationInboxContract {
    private static final Instant AT = Instant.parse("2026-11-01T08:00:00Z");
    private static final String INSERT = """
            INSERT INTO notifications (event_id, person_id, type, expedition_id, sent_at)
            VALUES (?, ?, 'ExpeditionApproved', ?, now())
            """;

    @Autowired
    private NotificationInbox inbox;
    @Autowired
    private EventLog log;
    @Autowired
    private PersonRegistry people;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager transactions;

    @Override
    protected void inTransaction(Runnable work) {
        new TransactionTemplate(transactions).executeWithoutResult(status -> work.run());
    }

    @Override
    protected NotificationInbox inbox() {
        return inbox;
    }

    @Override
    protected EventLog eventLog() {
        return log;
    }

    @Override
    protected PersonId registeredPerson() {
        PersonId id = new PersonId(UUID.randomUUID());
        inTransaction(() -> people.save(new Person(id, new PersonName("Ada"), List.of(), Availability.always())));
        return id;
    }

    @Test
    void thePrimaryKeyRejectsADuplicateRow() {
        UUID event = insertedEvent();
        UUID person = insertedPerson();
        jdbc.update(INSERT, event, person, UUID.randomUUID());

        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(INSERT, event, person, UUID.randomUUID()));
    }

    @Test
    void aNotificationNeedsAnExistingEvent() {
        UUID person = insertedPerson();

        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(INSERT, UUID.randomUUID(), person, UUID.randomUUID()));
    }

    @Test
    void aNotificationNeedsAKnownRecipient() {
        UUID event = insertedEvent();

        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(INSERT, event, UUID.randomUUID(), UUID.randomUUID()));
    }

    private UUID insertedPerson() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO people (id, name) VALUES (?, 'Ada')", id);
        return id;
    }

    private UUID insertedEvent() {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO domain_events (id, type, schema_version, expedition_id, occurred_at, recorded_at, payload)
                VALUES (?, 'RunStarted', 1, ?, now(), now(), '{}'::jsonb)
                """, id, UUID.randomUUID());
        return id;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RecordingTime {
        @Bean
        Clock clock() {
            return () -> AT;
        }
    }
}
