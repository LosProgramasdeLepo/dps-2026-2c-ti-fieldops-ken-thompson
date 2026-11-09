package edu.itba.fieldops.domain.notification;

import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.catalog.PersonName;
import edu.itba.fieldops.domain.event.ActivityCompleted;
import edu.itba.fieldops.domain.event.ActivityStarted;
import edu.itba.fieldops.domain.event.DomainEvent;
import edu.itba.fieldops.domain.event.EventType;
import edu.itba.fieldops.domain.event.ExpeditionApproved;
import edu.itba.fieldops.domain.event.IncidentReported;
import edu.itba.fieldops.domain.event.RecordedEvent;
import edu.itba.fieldops.domain.event.ReplanDecided;
import edu.itba.fieldops.domain.event.ReplanProposed;
import edu.itba.fieldops.domain.event.RunFinished;
import edu.itba.fieldops.domain.event.RunStarted;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionCharter;
import edu.itba.fieldops.domain.expedition.ExpeditionState;
import edu.itba.fieldops.domain.expedition.ExpeditionStatus;
import edu.itba.fieldops.domain.expedition.Objective;
import edu.itba.fieldops.domain.expedition.ReplanProposal;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.EventId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.IncidentId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.ProposalId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.ActivityName;
import edu.itba.fieldops.domain.itinerary.EstimatedDuration;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.support.StoredExpeditions;
import edu.itba.fieldops.domain.support.StoredPeople;
import edu.itba.fieldops.domain.tracking.IncidentDescription;
import edu.itba.fieldops.domain.tracking.Outcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponsiblesToNotifyTest {
    private static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");
    private static final WorkZone DELTA = new WorkZone("Delta");
    private static final PersonId ADA = new PersonId(UUID.randomUUID());
    private static final PersonId BOB = new PersonId(UUID.randomUUID());
    private static final PersonId EVE = new PersonId(UUID.randomUUID());
    private static final ExpeditionId PLAN = newId();
    private static final Instant SENT_AT = DAY.plus(Duration.ofMinutes(5));

    static Stream<DomainEvent> concerning() {
        return Stream.of(
                new ExpeditionApproved(PLAN, DAY, 1, Optional.empty()),
                new IncidentReported(PLAN, DAY, new IncidentId(UUID.randomUUID()), Optional.empty(), new IncidentDescription("storm")),
                new ReplanProposed(PLAN, DAY, new ProposalId(UUID.randomUUID()), newId(), new IncidentId(UUID.randomUUID()))
        );
    }

    static Stream<DomainEvent> notConcerning() {
        ActivityId activity = new ActivityId(UUID.randomUUID());
        return Stream.of(
                new RunStarted(PLAN, DAY),
                new ActivityStarted(PLAN, DAY, activity),
                new ActivityCompleted(PLAN, DAY, activity, DAY.minus(Duration.ofHours(1)), new Outcome("done"), Map.of()),
                new RunFinished(PLAN, DAY),
                new ReplanDecided(PLAN, DAY, new ProposalId(UUID.randomUUID()), ReplanProposal.Decision.REJECTED, ADA)
        );
    }

    @ParameterizedTest
    @MethodSource("concerning")
    void notifiesTheResponsiblesOfTheApprovalTheIncidentAndTheProposal(DomainEvent event) {
        Expedition plan = approved(PLAN, Optional.empty(), List.of(ADA, BOB));
        ResponsiblesToNotify responsibles = new ResponsiblesToNotify(new StoredExpeditions(plan));
        RecordedEvent recorded = recorded(event);

        assertTrue(responsibles.concern(event));
        assertEquals(
                List.of(
                        new Notification(recorded.id(), ADA, EventType.of(event), PLAN, SENT_AT),
                        new Notification(recorded.id(), BOB, EventType.of(event), PLAN, SENT_AT)
                ),
                responsibles.notificationsAbout(recorded, SENT_AT)
        );
    }

    @ParameterizedTest
    @MethodSource("notConcerning")
    void leavesTheOtherEventsAlone(DomainEvent event) {
        ResponsiblesToNotify responsibles = new ResponsiblesToNotify(new StoredExpeditions());

        assertFalse(responsibles.concern(event));
        assertEquals(List.of(), responsibles.notificationsAbout(recorded(event), SENT_AT));
    }

    @Test
    void notifiesTheResponsiblesOfTheRevisionInForce() {
        Expedition original = restored(approved(PLAN, Optional.empty(), List.of(ADA)), ExpeditionStatus.SUPERSEDED);
        Expedition revision = approved(newId(), Optional.of(PLAN), List.of(EVE));
        ResponsiblesToNotify responsibles = new ResponsiblesToNotify(new StoredExpeditions(original, revision));

        List<Notification> sent = responsibles.notificationsAbout(recorded(new ExpeditionApproved(PLAN, DAY, 1, Optional.empty())), SENT_AT);

        assertEquals(List.of(EVE), sent.stream().map(Notification::recipient).toList());
    }

    @Test
    void rejectsAnEventAboutAnUnknownPlan() {
        ResponsiblesToNotify responsibles = new ResponsiblesToNotify(new StoredExpeditions());

        RecordedEvent approval = recorded(new ExpeditionApproved(PLAN, DAY, 1, Optional.empty()));

        assertThrows(UnknownResource.class, () -> responsibles.notificationsAbout(approval, SENT_AT));
    }

    private static RecordedEvent recorded(DomainEvent event) {
        return new RecordedEvent(new EventId(UUID.randomUUID()), 1, DAY, event);
    }

    private static ExpeditionId newId() {
        return new ExpeditionId(UUID.randomUUID());
    }

    private static Expedition approved(ExpeditionId id, Optional<ExpeditionId> supersedes, List<PersonId> leads) {
        Expedition draft = Expedition.draft(
                id,
                new ExpeditionCharter(
                        List.of(new Objective("Map wetland")),
                        new TimePeriod(DAY, DAY.plus(Duration.ofDays(5))),
                        List.of(DELTA),
                        leads,
                        List.of()
                ),
                StoredPeople.of(leads.stream()
                        .map(lead -> new Person(lead, new PersonName("Lead"), List.of(), Availability.always()))
                        .toArray(Person[]::new))
        );
        draft.addActivity(Activity.sampling(new CertificationId(UUID.randomUUID()))
                .named(new ActivityId(UUID.randomUUID()), new ActivityName("Soil sampling"))
                .estimated(new EstimatedDuration(Duration.ofHours(4)), RiskLevel.MEDIUM)
                .in(DELTA, new TimePeriod(DAY, DAY.plus(Duration.ofHours(4))))
                .build());
        ExpeditionState state = draft.state();
        return Expedition.restore(new ExpeditionState(
                state.id(),
                supersedes.isPresent() ? 2 : 1,
                supersedes,
                ExpeditionStatus.APPROVED,
                state.charter(),
                state.items(),
                state.assignments(),
                state.permits(),
                state.acceptedWarnings()
        ));
    }

    private static Expedition restored(Expedition plan, ExpeditionStatus status) {
        ExpeditionState state = plan.state();
        return Expedition.restore(new ExpeditionState(
                state.id(),
                state.version(),
                state.supersedes(),
                status,
                state.charter(),
                state.items(),
                state.assignments(),
                state.permits(),
                state.acceptedWarnings()
        ));
    }
}
