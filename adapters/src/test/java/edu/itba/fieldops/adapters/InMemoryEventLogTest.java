package edu.itba.fieldops.adapters;

import edu.itba.fieldops.domain.event.EventFilter;
import edu.itba.fieldops.domain.event.RecordedEvent;
import edu.itba.fieldops.domain.event.RunFinished;
import edu.itba.fieldops.domain.event.RunStarted;
import edu.itba.fieldops.domain.identity.EventId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.shared.PageRequest;
import edu.itba.fieldops.domain.shared.UnknownResource;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryEventLogTest {
    private static final Instant NOW = Instant.parse("2026-11-01T09:00:00Z");
    private static final Instant AT = Instant.parse("2026-11-01T08:00:00Z");
    private static final ExpeditionId PLAN = new ExpeditionId(UUID.randomUUID());
    private static final ExpeditionId OTHER = new ExpeditionId(UUID.randomUUID());

    private final InMemoryEventLog log = new InMemoryEventLog(() -> NOW);

    @Test
    void recordsEventsInAscendingSequenceWithTheClocksTime() {
        log.append(List.of(new RunStarted(PLAN, AT), new RunFinished(PLAN, AT)));
        log.append(List.of(new RunStarted(OTHER, AT)));

        List<RecordedEvent> stored = log.page(EventFilter.all(), new PageRequest(0, 20)).items();

        assertAll(
                () -> assertEquals(List.of(1L, 2L, 3L), stored.stream().map(RecordedEvent::sequence).toList()),
                () -> assertEquals(3, stored.stream().map(RecordedEvent::id).distinct().count()),
                () -> assertEquals(List.of(NOW, NOW, NOW), stored.stream().map(RecordedEvent::recordedAt).toList())
        );
    }

    @Test
    void pagesAndFiltersByPlanAndType() {
        log.append(List.of(new RunStarted(PLAN, AT), new RunFinished(PLAN, AT), new RunStarted(OTHER, AT)));

        assertAll(
                () -> assertEquals(3, log.page(EventFilter.all(), new PageRequest(0, 2)).totalItems()),
                () -> assertEquals(1, log.page(EventFilter.all(), new PageRequest(1, 2)).items().size()),
                () -> assertEquals(2, log.page(EventFilter.forExpeditions(Set.of(PLAN)), new PageRequest(0, 20)).totalItems()),
                () -> assertEquals(2, log.page(EventFilter.all().withTypes(Set.of("RunStarted")), new PageRequest(0, 20)).totalItems()),
                () -> assertEquals(
                        1,
                        log.page(EventFilter.forExpeditions(Set.of(PLAN)).withTypes(Set.of("RunStarted")), new PageRequest(0, 20)).totalItems()
                )
        );
    }

    @Test
    void findsAnEventByIdAndRequiresItToExist() {
        log.append(List.of(new RunStarted(PLAN, AT)));
        RecordedEvent stored = log.page(EventFilter.all(), new PageRequest(0, 20)).items().getFirst();

        assertAll(
                () -> assertEquals(Optional.of(stored), log.find(stored.id())),
                () -> assertThrows(UnknownResource.class, () -> log.require(new EventId(UUID.randomUUID())))
        );
    }
}
