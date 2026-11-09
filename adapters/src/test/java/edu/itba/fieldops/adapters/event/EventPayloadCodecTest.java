package edu.itba.fieldops.adapters.event;

import edu.itba.fieldops.domain.event.ActivityCompleted;
import edu.itba.fieldops.domain.event.ActivityStarted;
import edu.itba.fieldops.domain.event.DomainEvent;
import edu.itba.fieldops.domain.event.EventTypes;
import edu.itba.fieldops.domain.event.ExpeditionApproved;
import edu.itba.fieldops.domain.event.IncidentReported;
import edu.itba.fieldops.domain.event.ReplanDecided;
import edu.itba.fieldops.domain.event.ReplanProposed;
import edu.itba.fieldops.domain.event.RunFinished;
import edu.itba.fieldops.domain.event.RunStarted;
import edu.itba.fieldops.domain.expedition.ReplanProposal;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.IncidentId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.ProposalId;
import edu.itba.fieldops.domain.shared.Consumption;
import edu.itba.fieldops.domain.tracking.IncidentDescription;
import edu.itba.fieldops.domain.tracking.Outcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventPayloadCodecTest {
    private static final Instant AT = Instant.parse("2026-11-01T08:00:00.123456Z");
    private static final ExpeditionId EXPEDITION = new ExpeditionId(new UUID(0, 1));
    private static final ExpeditionId OTHER = new ExpeditionId(new UUID(0, 2));
    private static final ActivityId ACTIVITY = new ActivityId(new UUID(1, 1));
    private static final IncidentId INCIDENT = new IncidentId(new UUID(3, 1));
    private static final ProposalId PROPOSAL = new ProposalId(new UUID(4, 1));
    private static final PersonId ADA = new PersonId(new UUID(5, 1));

    private final EventPayloadCodec codec = new EventPayloadCodec();

    static Stream<DomainEvent> events() {
        return Stream.of(
                new ExpeditionApproved(EXPEDITION, AT, 1, Optional.empty()),
                new ExpeditionApproved(EXPEDITION, AT, 2, Optional.of(OTHER)),
                new RunStarted(EXPEDITION, AT),
                new ActivityStarted(EXPEDITION, AT, ACTIVITY),
                new ActivityCompleted(EXPEDITION, AT, ACTIVITY, AT.minusSeconds(60), new Outcome("done"), Map.of()),
                new ActivityCompleted(EXPEDITION, AT, ACTIVITY, AT.minusSeconds(60), new Outcome("done"), Map.of(
                        new ConsumableId(new UUID(2, 1)), new Consumption(2),
                        new ConsumableId(new UUID(2, 2)), new Consumption(40),
                        new ConsumableId(new UUID(2, 3)), new Consumption(1)
                )),
                new IncidentReported(EXPEDITION, AT, INCIDENT, Optional.empty(), new IncidentDescription("storm")),
                new IncidentReported(EXPEDITION, AT, INCIDENT, Optional.of(ACTIVITY), new IncidentDescription("storm")),
                new RunFinished(EXPEDITION, AT),
                new ReplanProposed(EXPEDITION, AT, PROPOSAL, OTHER, INCIDENT),
                new ReplanDecided(EXPEDITION, AT, PROPOSAL, ReplanProposal.Decision.ACCEPTED, ADA),
                new ReplanDecided(EXPEDITION, AT, PROPOSAL, ReplanProposal.Decision.REJECTED, ADA)
        );
    }

    @ParameterizedTest
    @MethodSource("events")
    void decodingWhatWasEncodedGivesAnEqualEvent(DomainEvent event) {
        Map<String, Object> payload = codec.encode(event);

        assertEquals(event, codec.decode(event.type(), EventPayloadCodec.SCHEMA_VERSION, payload));
    }

    @ParameterizedTest
    @MethodSource("events")
    void numbersMayComeBackAsLongsFromTheStore(DomainEvent event) {
        Map<String, Object> payload = new HashMap<>(codec.encode(event));
        payload.replaceAll((key, value) -> value instanceof Integer number ? Long.valueOf(number) : value);
        if (payload.get("consumed") instanceof Map<?, ?> consumed) {
            Map<String, Object> widened = new HashMap<>();
            consumed.forEach((id, amount) -> widened.put(String.valueOf(id), ((Number) amount).longValue()));
            payload.put("consumed", widened);
        }

        assertEquals(event, codec.decode(event.type(), EventPayloadCodec.SCHEMA_VERSION, payload));
    }

    @Test
    void everyKnownTypeIsEncodedAndDecoded() {
        Set<String> covered = new HashSet<>();
        events().forEach(event -> covered.add(event.type()));

        assertEquals(EventTypes.known(), covered);
    }

    @Test
    void anAbsentOptionalIsAnAbsentKeyAndInstantsAreIsoStrings() {
        Map<String, Object> payload = codec.encode(new ExpeditionApproved(EXPEDITION, AT, 1, Optional.empty()));

        assertFalse(payload.containsKey("superseded"));
        assertEquals("2026-11-01T08:00:00.123456Z", payload.get("occurredAt"));
        assertTrue(payload.get("version") instanceof Integer);
    }

    @Test
    void consumedIsAMapFromConsumableIdToAmount() {
        ConsumableId water = new ConsumableId(new UUID(2, 1));
        Map<String, Object> payload = codec.encode(
                new ActivityCompleted(EXPEDITION, AT, ACTIVITY, AT, new Outcome("done"), Map.of(water, new Consumption(2)))
        );

        assertEquals(Map.of(water.value().toString(), 2), payload.get("consumed"));
    }

    @Test
    void anUnknownTypeIsRejected() {
        Map<String, Object> payload = codec.encode(new RunStarted(EXPEDITION, AT));

        assertThrows(IllegalStateException.class, () -> codec.decode("RunPaused", EventPayloadCodec.SCHEMA_VERSION, payload));
    }

    @Test
    void anUnknownSchemaVersionIsRejected() {
        Map<String, Object> payload = codec.encode(new RunStarted(EXPEDITION, AT));

        assertThrows(IllegalStateException.class, () -> codec.decode("RunStarted", 2, payload));
    }

    @Test
    void aMissingFieldIsRejected() {
        Map<String, Object> payload = new HashMap<>(codec.encode(new ActivityStarted(EXPEDITION, AT, ACTIVITY)));
        payload.remove("activityId");

        assertThrows(IllegalStateException.class, () -> codec.decode("ActivityStarted", EventPayloadCodec.SCHEMA_VERSION, payload));
    }
}
