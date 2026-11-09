package edu.itba.fieldops.frameworks;

import edu.itba.fieldops.adapters.messaging.NotifyResponsiblesConsumer;
import edu.itba.fieldops.adapters.messaging.ReplanOnIncident;
import edu.itba.fieldops.domain.event.Delivery;
import edu.itba.fieldops.domain.event.DeliveryStatus;
import edu.itba.fieldops.domain.event.EventFilter;
import edu.itba.fieldops.domain.event.EventType;
import edu.itba.fieldops.domain.event.RecordedEvent;
import edu.itba.fieldops.domain.expedition.ExpeditionCharter;
import edu.itba.fieldops.domain.expedition.Objective;
import edu.itba.fieldops.domain.expedition.PersonAssignment;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.IncidentId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.notification.Notification;
import edu.itba.fieldops.domain.shared.PermitKind;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.domain.tracking.IncidentDescription;
import edu.itba.fieldops.domain.usecase.notification.ConsultNotifications;
import edu.itba.fieldops.domain.usecase.notification.NotifyResponsibles;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificationUseCasesTest extends UseCaseFixture {
    private static final IncidentDescription STORM = new IncidentDescription("storm on site");
    private static final String SIMULATE_FAILURE = "fieldops.notifications.simulate-failure";

    @Autowired
    private ConsultNotifications inbox;
    @Autowired
    private NotifyResponsibles notify;
    @Autowired
    private ConfigurableApplicationContext context;

    private List<Notification> inboxOf(PersonId person) {
        return inbox.of(person, FIRST_PAGE).items();
    }

    private List<String> typesIn(PersonId person) {
        return inboxOf(person).stream().map(notification -> notification.type().value()).toList();
    }

    private RecordedEvent recorded(ExpeditionId expeditionId, String type) {
        return eventLog.page(EventFilter.forExpeditions(Set.of(expeditionId)).withTypes(Set.of(type)), FIRST_PAGE)
                .items().getFirst();
    }

    private IncidentId incidentOn(Sampling sampling) {
        clock.set(at(2));
        return incidents.record(sampling.expeditionId(), STORM, sampling.activityId());
    }

    private Delivery deliveryTo(String consumer, Sampling sampling, IncidentId incidentId) {
        return incidents.incident(sampling.expeditionId(), incidentId).processing().stream()
                .filter(delivery -> delivery.consumer().equals(consumer))
                .findFirst()
                .orElseThrow();
    }

    private void simulateFailure(boolean on) {
        Map<String, String> changed = Map.of(SIMULATE_FAILURE, Boolean.toString(on));
        TestPropertyValues.of(changed).applyTo(context);
        context.publishEvent(new EnvironmentChangeEvent(context, changed.keySet()));
    }

    @Test
    void anApprovalReachesTheResponsibleOnlyOnceTheEventIsDispatched() {
        Sampling sampling = approvedSampling();
        List<Notification> beforeTheDispatch = inboxOf(sampling.responsible());

        dispatcher.dispatchDue();

        assertAll(
                () -> assertEquals(List.of(), beforeTheDispatch),
                () -> assertEquals(
                        List.of(new Notification(
                                recorded(sampling.expeditionId(), "ExpeditionApproved").id(),
                                sampling.responsible(),
                                new EventType("ExpeditionApproved"),
                                sampling.expeditionId(),
                                DAY
                        )),
                        inboxOf(sampling.responsible())
                )
        );
    }

    @Test
    void everyResponsibleOfThePlanIsNotifiedAndNobodyElse() {
        CertificationId certificationId = certification();
        PersonId ada = certifiedPerson("Ada", certificationId);
        PersonId bob = certifiedPerson("Bob", certificationId);
        PersonId eve = certifiedPerson("Eve", certificationId);
        ExpeditionId expeditionId = drafts.draft(new ExpeditionCharter(
                List.of(new Objective("Map wetland")), PERIOD, List.of(DELTA), List.of(ada, bob), List.of()
        ));
        ActivityId activityId = itinerary.nextActivityId();
        itinerary.addActivity(expeditionId, sampling(certificationId, activityId, hours(0, 4)));
        assignments.addAssignment(expeditionId, new PersonAssignment(activityId, eve));
        assignments.addPermit(expeditionId, permitting.registerPermit(PermitKind.ZONE, DELTA, PERIOD));
        review.submit(expeditionId);
        approval.approve(expeditionId);

        dispatcher.dispatchDue();

        assertAll(
                () -> assertEquals(List.of("ExpeditionApproved"), typesIn(ada)),
                () -> assertEquals(List.of("ExpeditionApproved"), typesIn(bob)),
                () -> assertEquals(List.of(), typesIn(eve))
        );
    }

    @Test
    void anIncidentAndItsProposalReachTheResponsibleNewestFirst() {
        Sampling sampling = runningSampling();
        dispatcher.dispatchDue();
        incidentOn(sampling);

        dispatcher.dispatchDue();
        dispatcher.dispatchDue();

        assertEquals(List.of("ReplanProposed", "IncidentReported", "ExpeditionApproved"), typesIn(sampling.responsible()));
    }

    @Test
    void theSameEventDeliveredAgainSendsNoSecondNotification() {
        Sampling sampling = approvedSampling();
        dispatcher.dispatchDue();

        notify.about(recorded(sampling.expeditionId(), "ExpeditionApproved"));

        assertEquals(1, inboxOf(sampling.responsible()).size());
    }

    @Test
    void theInboxOfAnUnknownPersonIsAnUnknownResource() {
        PersonId ghost = new PersonId(UUID.randomUUID());

        assertThrows(UnknownResource.class, () -> inbox.of(ghost, FIRST_PAGE));
    }

    @Test
    void aFailingNotificationRetriesWithoutStoppingTheReplan() {
        Sampling sampling = runningSampling();
        simulateFailure(true);
        IncidentId incidentId = incidentOn(sampling);

        dispatcher.dispatchDue();

        assertAll(
                () -> assertEquals(DeliveryStatus.RETRYING, deliveryTo(NotifyResponsiblesConsumer.NAME, sampling, incidentId).status()),
                () -> assertEquals(DeliveryStatus.DELIVERED, deliveryTo(ReplanOnIncident.NAME, sampling, incidentId).status()),
                () -> assertEquals(1, proposalReview.of(sampling.expeditionId(), FIRST_PAGE).items().size()),
                () -> assertEquals(List.of(), inboxOf(sampling.responsible()))
        );
    }

    @Test
    void turningTheFailureOffDeliversTheNotificationOnceAfterTheBackoff() {
        Sampling sampling = runningSampling();
        simulateFailure(true);
        IncidentId incidentId = incidentOn(sampling);
        dispatcher.dispatchDue();

        simulateFailure(false);
        clock.set(clock.now().plus(Duration.ofSeconds(1)));
        dispatcher.dispatchDue();

        assertAll(
                () -> assertEquals(DeliveryStatus.DELIVERED, deliveryTo(NotifyResponsiblesConsumer.NAME, sampling, incidentId).status()),
                () -> assertEquals(
                        1,
                        typesIn(sampling.responsible()).stream().filter("IncidentReported"::equals).count()
                )
        );
    }
}
