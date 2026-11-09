package edu.itba.fieldops.adapters.mongo.event;

import edu.itba.fieldops.adapters.EventDeliveriesContract;
import edu.itba.fieldops.adapters.PersistenceProfiles;
import edu.itba.fieldops.adapters.mongo.MongoRepositoryTest;
import edu.itba.fieldops.adapters.mongo.RegistrationSequence;
import edu.itba.fieldops.domain.event.EventDeliveries;
import edu.itba.fieldops.domain.event.EventLog;
import edu.itba.fieldops.domain.identity.EventId;
import edu.itba.fieldops.domain.shared.Clock;
import edu.itba.fieldops.domain.shared.UnknownResource;
import org.bson.Document;
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
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MongoRepositoryTest
@ActiveProfiles(PersistenceProfiles.MONGO)
@Import({MongoEventLog.class, MongoEventDeliveries.class, RegistrationSequence.class, MongoEventDeliveriesTest.RecordingTime.class})
class MongoEventDeliveriesTest extends EventDeliveriesContract {
    private static final Instant AT = Instant.parse("2026-11-01T08:00:00Z");

    @Autowired
    private EventDeliveries deliveries;
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
    protected EventDeliveries deliveries() {
        return deliveries;
    }

    @Override
    protected EventLog eventLog() {
        return log;
    }

    @Test
    void succeededRequiresAnOpenTransaction() {
        EventId id = new EventId(UUID.randomUUID());

        assertThrows(IllegalTransactionStateException.class, () -> deliveries.succeeded(id, "alpha", AT));
    }

    @Test
    void schedulingAnUnknownEventFails() {
        EventId unknown = new EventId(UUID.randomUUID());

        assertThrows(UnknownResource.class, () -> inTransaction(() -> deliveries.schedule(unknown, Set.of("alpha"), AT)));
    }

    @Test
    void theCompositeIdIsStoredAsASubdocument() {
        EventId id = new EventId(UUID.randomUUID());
        Document stored = new Document("_id", new Document("eventId", id.value().toString()).append("consumer", "alpha"))
                .append("sequence", 1L).append("status", "DELIVERED").append("attempts", 1);
        mongo.getCollection(DeliveryDocument.COLLECTION).insertOne(stored);

        assertEquals("alpha", deliveries.of(id).getFirst().consumer());
    }

    @Test
    void theDeliveryIndexesExist() {
        List<String> names = mongo.indexOps(DeliveryDocument.class).getIndexInfo().stream().map(IndexInfo::getName).toList();

        assertTrue(names.containsAll(List.of("status_next_attempt", "event")), names::toString);
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
