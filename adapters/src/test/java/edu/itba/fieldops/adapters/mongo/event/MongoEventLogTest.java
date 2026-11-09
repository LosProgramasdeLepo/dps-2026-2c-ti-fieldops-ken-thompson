package edu.itba.fieldops.adapters.mongo.event;

import edu.itba.fieldops.adapters.PersistenceProfiles;
import edu.itba.fieldops.adapters.mongo.MongoRepositoryTest;
import edu.itba.fieldops.adapters.mongo.RegistrationSequence;
import edu.itba.fieldops.domain.event.ActivityCompleted;
import edu.itba.fieldops.domain.event.ActivityStarted;
import edu.itba.fieldops.domain.event.DomainEvent;
import edu.itba.fieldops.domain.event.EventFilter;
import edu.itba.fieldops.domain.event.EventLog;
import edu.itba.fieldops.domain.event.ExpeditionApproved;
import edu.itba.fieldops.domain.event.IncidentReported;
import edu.itba.fieldops.domain.event.RecordedEvent;
import edu.itba.fieldops.domain.event.ReplanDecided;
import edu.itba.fieldops.domain.event.ReplanProposed;
import edu.itba.fieldops.domain.event.RunFinished;
import edu.itba.fieldops.domain.event.RunStarted;
import edu.itba.fieldops.domain.expedition.ReplanProposal;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.EventId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.IncidentId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.ProposalId;
import edu.itba.fieldops.domain.shared.Clock;
import edu.itba.fieldops.domain.shared.Consumption;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.domain.tracking.IncidentDescription;
import edu.itba.fieldops.domain.tracking.Outcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexField;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MongoRepositoryTest
@ActiveProfiles(PersistenceProfiles.MONGO)
@Import({MongoEventLog.class, RegistrationSequence.class, MongoEventLogTest.RecordingTime.class})
class MongoEventLogTest {
    private static final Instant RECORDED_AT = Instant.parse("2026-11-01T09:00:00.123456Z");
    private static final Instant AT = Instant.parse("2026-11-01T08:00:00.654321Z");
    private static final ExpeditionId PLAN = new ExpeditionId(UUID.randomUUID());
    private static final ExpeditionId REVISION = new ExpeditionId(UUID.randomUUID());
    private static final ExpeditionId OTHER = new ExpeditionId(UUID.randomUUID());
    private static final PageRequest FIRST = new PageRequest(0, 100);

    @Autowired
    private EventLog log;
    @Autowired
    private MongoTemplate mongo;
    @Autowired
    private PlatformTransactionManager transactions;

    private TransactionTemplate transaction;

    @BeforeEach
    void startEmpty() {
        transaction = new TransactionTemplate(transactions);
        mongo.remove(new Query(), EventDocument.COLLECTION);
        mongo.remove(new Query(), "registration_sequences");
    }

    private void append(List<DomainEvent> events) {
        transaction.executeWithoutResult(status -> log.append(events));
    }

    private static List<DomainEvent> oneOfEach() {
        ActivityId activity = new ActivityId(UUID.randomUUID());
        IncidentId incident = new IncidentId(UUID.randomUUID());
        ProposalId proposal = new ProposalId(UUID.randomUUID());
        return List.of(
                new ExpeditionApproved(PLAN, AT, 1, Optional.empty()),
                new RunStarted(PLAN, AT),
                new ActivityStarted(PLAN, AT, activity),
                new ActivityCompleted(PLAN, AT, activity, AT.minusSeconds(30), new Outcome("done"), Map.of(
                        new ConsumableId(UUID.randomUUID()), new Consumption(3),
                        new ConsumableId(UUID.randomUUID()), new Consumption(1)
                )),
                new IncidentReported(PLAN, AT, incident, Optional.of(activity), new IncidentDescription("storm")),
                new ReplanProposed(PLAN, AT, proposal, REVISION, incident),
                new ReplanDecided(PLAN, AT, proposal, ReplanProposal.Decision.REJECTED, new PersonId(UUID.randomUUID())),
                new RunFinished(PLAN, AT),
                new ExpeditionApproved(REVISION, AT, 2, Optional.of(PLAN))
        );
    }

    @Test
    void everyTypeComesBackEqualInTheOrderItWasAppended() {
        List<DomainEvent> events = oneOfEach();

        append(events);

        List<RecordedEvent> stored = log.page(EventFilter.all(), FIRST).items();
        assertAll(
                () -> assertEquals(events, stored.stream().map(RecordedEvent::event).toList()),
                () -> assertTrue(stored.stream().allMatch(item -> item.recordedAt().equals(RECORDED_AT))),
                () -> assertEquals(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L), stored.stream().map(RecordedEvent::sequence).toList())
        );
    }

    @Test
    void findsAnEventByIdAndRequiresItToExist() {
        append(List.of(new RunStarted(PLAN, AT)));
        RecordedEvent stored = log.page(EventFilter.all(), FIRST).items().getFirst();

        assertAll(
                () -> assertEquals(Optional.of(stored), log.find(stored.id())),
                () -> assertEquals(Optional.empty(), log.find(new EventId(UUID.randomUUID()))),
                () -> assertThrows(UnknownResource.class, () -> log.require(new EventId(UUID.randomUUID())))
        );
    }

    @Test
    void pagesAFamilyOfPlansWithTotals() {
        append(oneOfEach());
        append(List.of(new RunStarted(OTHER, AT)));

        Page<RecordedEvent> family = log.page(EventFilter.forExpeditions(Set.of(PLAN, REVISION)), new PageRequest(0, 4));
        Page<RecordedEvent> rest = log.page(EventFilter.forExpeditions(Set.of(PLAN, REVISION)), new PageRequest(2, 4));
        Page<RecordedEvent> everything = log.page(EventFilter.all(), new PageRequest(0, 2));

        assertAll(
                () -> assertEquals(9, family.totalItems()),
                () -> assertEquals(3, family.totalPages()),
                () -> assertEquals(4, family.items().size()),
                () -> assertEquals(1, rest.items().size()),
                () -> assertEquals(10, everything.totalItems()),
                () -> assertEquals(2, everything.items().size())
        );
    }

    @Test
    void filtersByType() {
        append(oneOfEach());

        Page<RecordedEvent> approvals = log.page(EventFilter.all().withTypes(Set.of("ExpeditionApproved")), FIRST);
        Page<RecordedEvent> planApprovals = log.page(
                EventFilter.forExpeditions(Set.of(PLAN)).withTypes(Set.of("ExpeditionApproved", "RunStarted")), FIRST
        );

        assertAll(
                () -> assertEquals(2, approvals.totalItems()),
                () -> assertEquals(List.of("ExpeditionApproved", "RunStarted"), planApprovals.items().stream().map(item -> item.event().type()).toList())
        );
    }

    @Test
    void aRolledBackAppendLeavesNothing() {
        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
            log.append(List.of(new RunStarted(PLAN, AT)));
            throw new IllegalStateException("boom");
        }));

        assertEquals(0, log.page(EventFilter.all(), FIRST).totalItems());
    }

    @Test
    void appendingOutsideATransactionFails() {
        assertThrows(IllegalTransactionStateException.class, () -> log.append(List.of(new RunStarted(PLAN, AT))));
    }

    @Test
    void theIndexesExist() {
        List<IndexInfo> indexes = mongo.indexOps(EventDocument.COLLECTION).getIndexInfo();

        assertAll(
                () -> assertTrue(indexes.stream().anyMatch(index -> index.isUnique() && fields(index).equals(List.of("sequence")))),
                () -> assertTrue(indexes.stream().anyMatch(index -> fields(index).equals(List.of("expeditionId", "sequence")))),
                () -> assertTrue(indexes.stream().anyMatch(index -> fields(index).equals(List.of("type", "sequence"))))
        );
    }

    private static List<String> fields(IndexInfo index) {
        return index.getIndexFields().stream().map(IndexField::getKey).toList();
    }

    @Test
    void twoTransactionsCanAppendAtTheSameTime() throws Exception {
        CyclicBarrier bothAppended = new CyclicBarrier(2);
        List<Throwable> failures = new CopyOnWriteArrayList<>();
        Runnable appendAndWait = () -> {
            try {
                transaction.executeWithoutResult(status -> {
                    log.append(List.of(new RunStarted(PLAN, AT)));
                    awaitBoth(bothAppended);
                });
            } catch (RuntimeException failure) {
                failures.add(failure);
            }
        };

        Thread request = Thread.ofPlatform().start(appendAndWait);
        Thread dispatcher = Thread.ofPlatform().start(appendAndWait);
        request.join();
        dispatcher.join();

        assertAll(
                () -> assertEquals(List.of(), failures),
                () -> assertEquals(2, log.page(EventFilter.forExpeditions(Set.of(PLAN)), FIRST).totalItems())
        );
    }

    private static void awaitBoth(CyclicBarrier barrier) {
        try {
            barrier.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException | BrokenBarrierException | TimeoutException interrupted) {
            throw new IllegalStateException("the other transaction never appended", interrupted);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RecordingTime {
        @Bean
        MongoTransactionManager transactionManager(MongoDatabaseFactory databases) {
            return new MongoTransactionManager(databases);
        }

        @Bean
        Clock clock() {
            return () -> RECORDED_AT;
        }
    }
}
