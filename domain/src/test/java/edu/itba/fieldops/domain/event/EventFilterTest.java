package edu.itba.fieldops.domain.event;

import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.shared.InvalidValue;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventFilterTest {
    private static final ExpeditionId PLAN = new ExpeditionId(UUID.randomUUID());

    @Test
    void allMeansNoFilter() {
        EventFilter all = EventFilter.all();

        assertAll(
                () -> assertEquals(Set.of(), all.expeditions()),
                () -> assertEquals(Set.of(), all.types())
        );
    }

    @Test
    void combinesExpeditionsAndTypes() {
        EventFilter filter = EventFilter.forExpeditions(Set.of(PLAN)).withTypes(Set.of("RunStarted"));

        assertAll(
                () -> assertEquals(Set.of(PLAN), filter.expeditions()),
                () -> assertEquals(Set.of("RunStarted"), filter.types())
        );
    }

    @Test
    void rejectsAnUnknownType() {
        assertThrows(InvalidValue.class, () -> EventFilter.all().withTypes(Set.of("RunStarted", "Nope")));
    }

    @Test
    void copiesItsSetsDefensively() {
        Set<ExpeditionId> expeditions = new HashSet<>(Set.of(PLAN));
        EventFilter filter = EventFilter.forExpeditions(expeditions);

        expeditions.clear();

        assertEquals(Set.of(PLAN), filter.expeditions());
    }
}
