package edu.itba.fieldops.domain.event;

import edu.itba.fieldops.domain.expedition.ReplanProposal;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.IncidentId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.ProposalId;
import edu.itba.fieldops.domain.shared.Consumption;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.tracking.IncidentDescription;
import edu.itba.fieldops.domain.tracking.Outcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainEventsTest {
    private static final Instant AT = Instant.parse("2026-11-01T08:00:00Z");
    private static final ExpeditionId EXPEDITION = new ExpeditionId(new UUID(0, 1));
    private static final ExpeditionId OTHER = new ExpeditionId(new UUID(0, 2));
    private static final ActivityId ACTIVITY = new ActivityId(new UUID(1, 1));
    private static final ConsumableId WATER = new ConsumableId(new UUID(2, 1));
    private static final IncidentId INCIDENT = new IncidentId(new UUID(3, 1));
    private static final ProposalId PROPOSAL = new ProposalId(new UUID(4, 1));
    private static final PersonId ADA = new PersonId(new UUID(5, 1));

    static Stream<DomainEvent> examples() {
        return Stream.of(
                new ExpeditionApproved(EXPEDITION, AT, 2, Optional.of(OTHER)),
                new RunStarted(EXPEDITION, AT),
                new ActivityStarted(EXPEDITION, AT, ACTIVITY),
                new ActivityCompleted(EXPEDITION, AT, ACTIVITY, AT.minusSeconds(60), new Outcome("done"), Map.of(WATER, new Consumption(2))),
                new IncidentReported(EXPEDITION, AT, INCIDENT, Optional.of(ACTIVITY), new IncidentDescription("storm")),
                new RunFinished(EXPEDITION, AT),
                new ReplanProposed(EXPEDITION, AT, PROPOSAL, OTHER, INCIDENT),
                new ReplanDecided(EXPEDITION, AT, PROPOSAL, ReplanProposal.Decision.ACCEPTED, ADA)
        );
    }

    @ParameterizedTest
    @MethodSource("examples")
    void rejectsANullInEveryField(DomainEvent example) throws Exception {
        Class<?> type = example.getClass();
        RecordComponent[] components = type.getRecordComponents();
        Constructor<?> constructor = type.getDeclaredConstructor(
                java.util.Arrays.stream(components).map(RecordComponent::getType).toArray(Class[]::new)
        );
        for (int nulled = 0; nulled < components.length; nulled++) {
            if (components[nulled].getType().isPrimitive()) {
                continue;
            }
            Object[] values = new Object[components.length];
            for (int index = 0; index < components.length; index++) {
                values[index] = index == nulled ? null : components[index].getAccessor().invoke(example);
            }
            String field = components[nulled].getName();
            InvocationTargetException thrown = assertThrows(
                    InvocationTargetException.class,
                    () -> constructor.newInstance(values),
                    type.getSimpleName() + "." + field
            );
            assertInstanceOf(NullPointerException.class, thrown.getCause(), type.getSimpleName() + "." + field);
        }
    }

    @ParameterizedTest
    @MethodSource("examples")
    void typeIsTheSimpleName(DomainEvent example) {
        assertEquals(example.getClass().getSimpleName(), example.type());
    }

    @ParameterizedTest
    @MethodSource("examples")
    void everyEventCarriesItsExpeditionAndInstant(DomainEvent example) {
        assertEquals(EXPEDITION, example.expeditionId());
        assertEquals(AT, example.occurredAt());
    }

    @Test
    void knowsExactlyTheEightEventTypes() {
        Set<String> expected = examples().map(DomainEvent::type).collect(Collectors.toSet());

        assertEquals(8, expected.size());
        assertEquals(expected, EventTypes.known());
    }

    @Test
    void comparesByValue() {
        assertEquals(new RunStarted(EXPEDITION, AT), new RunStarted(EXPEDITION, AT));
        assertEquals(
                new ActivityCompleted(EXPEDITION, AT, ACTIVITY, AT, new Outcome("done"), Map.of(WATER, new Consumption(2))),
                new ActivityCompleted(EXPEDITION, AT, ACTIVITY, AT, new Outcome("done"), Map.of(WATER, new Consumption(2)))
        );
        assertTrue(!new RunStarted(EXPEDITION, AT).equals(new RunStarted(OTHER, AT)));
    }

    @Test
    void copiesTheConsumptionDefensively() {
        Map<ConsumableId, Consumption> consumed = new HashMap<>(Map.of(WATER, new Consumption(2)));
        ActivityCompleted event = new ActivityCompleted(EXPEDITION, AT, ACTIVITY, AT, new Outcome("done"), consumed);

        consumed.put(new ConsumableId(UUID.randomUUID()), new Consumption(1));

        assertEquals(Map.of(WATER, new Consumption(2)), event.consumed());
        assertThrows(UnsupportedOperationException.class, () -> event.consumed().clear());
    }

    @Test
    void rejectsAnApprovalWithoutVersion() {
        assertThrows(InvalidValue.class, () -> new ExpeditionApproved(EXPEDITION, AT, 0, Optional.empty()));
    }

    @Test
    void rejectsADecisionThatIsStillPending() {
        assertThrows(InvalidValue.class, () -> new ReplanDecided(EXPEDITION, AT, PROPOSAL, ReplanProposal.Decision.PENDING, ADA));
    }

    @Test
    void drainReturnsACopyAndEmptiesThePendingEvents() {
        PendingEvents pending = new PendingEvents();
        pending.record(new RunStarted(EXPEDITION, AT));

        List<DomainEvent> first = pending.drain();

        assertEquals(List.of(new RunStarted(EXPEDITION, AT)), first);
        assertEquals(List.of(), pending.drain());
        assertEquals(1, first.size());
        assertThrows(NullPointerException.class, () -> pending.record(null));
    }
}
