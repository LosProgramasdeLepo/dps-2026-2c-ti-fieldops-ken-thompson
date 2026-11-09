package edu.itba.fieldops.adapters.jpa.event;

import edu.itba.fieldops.adapters.PersistenceProfiles;
import edu.itba.fieldops.adapters.jpa.JpaRepositoryTest;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JpaRepositoryTest
@ActiveProfiles(PersistenceProfiles.SQL)
@Import({JpaEventLog.class, EventLogRepositoryTest.RecordingTime.class})
class EventLogRepositoryTest {
    private static final Instant RECORDED_AT = Instant.parse("2026-11-01T09:00:00.123456Z");
    private static final Instant AT = Instant.parse("2026-11-01T08:00:00.654321Z");
    private static final ExpeditionId PLAN = new ExpeditionId(UUID.randomUUID());
    private static final ExpeditionId REVISION = new ExpeditionId(UUID.randomUUID());
    private static final ExpeditionId OTHER = new ExpeditionId(UUID.randomUUID());
    private static final PageRequest FIRST = new PageRequest(0, 100);

    @Autowired
    private EventLog log;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private TestEntityManager entities;

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
                new ReplanDecided(PLAN, AT, proposal, ReplanProposal.Decision.ACCEPTED, new PersonId(UUID.randomUUID())),
                new RunFinished(PLAN, AT),
                new ExpeditionApproved(REVISION, AT, 2, Optional.of(PLAN))
        );
    }

    @Test
    void everyTypeComesBackEqualInTheOrderItWasAppended() {
        List<DomainEvent> events = oneOfEach();

        log.append(events);

        List<RecordedEvent> stored = log.page(EventFilter.all(), FIRST).items();
        assertAll(
                () -> assertEquals(events, stored.stream().map(RecordedEvent::event).toList()),
                () -> assertTrue(stored.stream().allMatch(item -> item.recordedAt().equals(RECORDED_AT))),
                () -> assertEquals(stored.stream().map(RecordedEvent::sequence).sorted().toList(), stored.stream().map(RecordedEvent::sequence).toList()),
                () -> assertEquals(stored.size(), stored.stream().map(RecordedEvent::sequence).distinct().count())
        );
    }

    @Test
    void findsAnEventByIdAndRequiresItToExist() {
        log.append(List.of(new RunStarted(PLAN, AT)));
        RecordedEvent stored = log.page(EventFilter.all(), FIRST).items().getFirst();

        assertAll(
                () -> assertEquals(Optional.of(stored), log.find(stored.id())),
                () -> assertEquals(stored, log.require(stored.id())),
                () -> assertEquals(Optional.empty(), log.find(new EventId(UUID.randomUUID()))),
                () -> assertThrows(UnknownResource.class, () -> log.require(new EventId(UUID.randomUUID())))
        );
    }

    @Test
    void pagesAFamilyOfPlansWithTotals() {
        log.append(oneOfEach());
        log.append(List.of(new RunStarted(OTHER, AT)));

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
        log.append(oneOfEach());

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
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void appendingOutsideATransactionFails() {
        assertThrows(IllegalTransactionStateException.class, () -> log.append(List.of(new RunStarted(PLAN, AT))));
    }

    @Test
    void anEventCannotBeUpdated() {
        log.append(List.of(new RunStarted(PLAN, AT)));
        entities.flush();

        assertThrows(DataAccessException.class, () -> jdbc.update("UPDATE domain_events SET type = 'RunFinished'"));
    }

    @Test
    void anEventCannotBeDeleted() {
        log.append(List.of(new RunStarted(PLAN, AT)));
        entities.flush();

        assertThrows(DataAccessException.class, () -> jdbc.update("DELETE FROM domain_events"));
    }

    @Test
    void theLogCannotBeTruncated() {
        assertThrows(DataAccessException.class, () -> jdbc.execute("TRUNCATE domain_events"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RecordingTime {
        @Bean
        Clock clock() {
            return () -> RECORDED_AT;
        }
    }
}
