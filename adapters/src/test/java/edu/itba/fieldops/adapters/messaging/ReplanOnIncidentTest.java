package edu.itba.fieldops.adapters.messaging;

import edu.itba.fieldops.domain.event.IncidentReported;
import edu.itba.fieldops.domain.event.RecordedEvent;
import edu.itba.fieldops.domain.event.RunStarted;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.EventId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.IncidentId;
import edu.itba.fieldops.domain.tracking.IncidentDescription;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplanOnIncidentTest {
    private static final Instant AT = Instant.parse("2026-11-01T08:00:00Z");
    private static final ExpeditionId RUN = new ExpeditionId(UUID.randomUUID());
    private static final IncidentId INCIDENT = new IncidentId(UUID.randomUUID());
    private static final IncidentDescription STORM = new IncidentDescription("storm");

    private final List<String> responses = new ArrayList<>();
    private final ReplanOnIncident consumer = new ReplanOnIncident((runId, incidentId) -> responses.add(runId + "/" + incidentId));

    @Test
    void reactsToAnIncidentOnAnActivity() {
        assertTrue(consumer.subscribesTo(affecting()));
    }

    @Test
    void ignoresAGeneralIncident() {
        assertFalse(consumer.subscribesTo(new IncidentReported(RUN, AT, INCIDENT, Optional.empty(), STORM)));
    }

    @Test
    void ignoresOtherEvents() {
        assertFalse(consumer.subscribesTo(new RunStarted(RUN, AT)));
    }

    @Test
    void passesTheRunAndTheIncidentToTheUseCase() {
        consumer.consume(new RecordedEvent(new EventId(UUID.randomUUID()), 1, AT, affecting()));

        assertEquals(List.of(RUN + "/" + INCIDENT), responses);
    }

    @Test
    void hasAStableName() {
        assertEquals("replan-on-incident", consumer.name());
    }

    private static IncidentReported affecting() {
        return new IncidentReported(RUN, AT, INCIDENT, Optional.of(new ActivityId(UUID.randomUUID())), STORM);
    }
}
