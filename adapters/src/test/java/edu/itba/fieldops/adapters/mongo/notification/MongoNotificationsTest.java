package edu.itba.fieldops.adapters.mongo.notification;

import edu.itba.fieldops.adapters.NotificationInboxContract;
import edu.itba.fieldops.adapters.PersistenceProfiles;
import edu.itba.fieldops.adapters.mongo.MongoRepositoryTest;
import edu.itba.fieldops.adapters.mongo.RegistrationSequence;
import edu.itba.fieldops.adapters.mongo.event.MongoEventLog;
import edu.itba.fieldops.adapters.notification.NotificationInbox;
import edu.itba.fieldops.domain.event.EventLog;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.shared.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@MongoRepositoryTest
@ActiveProfiles(PersistenceProfiles.MONGO)
@Import({MongoEventLog.class, MongoNotificationInbox.class, RegistrationSequence.class, MongoNotificationsTest.RecordingTime.class})
class MongoNotificationsTest extends NotificationInboxContract {
    private static final Instant AT = Instant.parse("2026-11-01T08:00:00Z");

    @Autowired
    private NotificationInbox inbox;
    @Autowired
    private EventLog log;
    @Autowired
    private MongoTemplate mongo;
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
        return new PersonId(UUID.randomUUID());
    }

    @Test
    void theInboxIndexExists() {
        List<String> names = mongo.indexOps(NotificationDocument.class).getIndexInfo().stream().map(IndexInfo::getName).toList();

        assertTrue(names.contains("inbox"), names::toString);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RecordingTime {
        @Bean
        MongoTransactionManager transactionManager(MongoDatabaseFactory databases) {
            return new MongoTransactionManager(databases);
        }

        @Bean
        Clock clock() {
            return () -> AT;
        }
    }
}
