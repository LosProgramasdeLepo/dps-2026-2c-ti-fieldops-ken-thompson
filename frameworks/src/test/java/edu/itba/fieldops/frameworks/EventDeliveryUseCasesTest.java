package edu.itba.fieldops.frameworks;

import edu.itba.fieldops.adapters.messaging.NotifyResponsiblesConsumer;
import edu.itba.fieldops.adapters.messaging.ReplanOnIncident;
import edu.itba.fieldops.domain.event.Delivery;
import edu.itba.fieldops.domain.event.DeliveryStatus;
import edu.itba.fieldops.domain.event.EventFilter;
import edu.itba.fieldops.domain.event.IncidentReported;
import edu.itba.fieldops.domain.event.InvalidDeliveryTransition;
import edu.itba.fieldops.domain.event.RecordedEvent;
import edu.itba.fieldops.domain.event.ReplanProposed;
import edu.itba.fieldops.domain.expedition.ReplanProposal;
import edu.itba.fieldops.domain.identity.EventId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.IncidentId;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.PageRequest;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.domain.tracking.IncidentDescription;
import edu.itba.fieldops.domain.usecase.audit.ConsultEvents;
import edu.itba.fieldops.domain.usecase.audit.ManageEventDeliveries;
import edu.itba.fieldops.domain.usecase.expedition.IncidentSnapshot;
import edu.itba.fieldops.domain.usecase.expedition.ProposalSnapshot;
import edu.itba.fieldops.domain.usecase.expedition.RespondToIncident;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;

class EventDeliveryUseCasesTest extends UseCaseFixture {
    private static final IncidentDescription STORM = new IncidentDescription("storm on site");

    @MockitoSpyBean
    private RespondToIncident responder;
    @Autowired
    private ReplanOnIncident replanOnIncident;
    @Autowired
    private ManageEventDeliveries manage;
    @Autowired
    private ConsultEvents audit;

    private IncidentId incidentOn(Sampling sampling) {
        clock.set(at(2));
        return incidents.record(sampling.expeditionId(), STORM, sampling.activityId());
    }

    private void respondingFails() {
        doThrow(new IllegalStateException("replan unavailable")).when(responder).respondTo(any(), any());
    }

    private void respondingWorks() {
        doCallRealMethod().when(responder).respondTo(any(), any());
    }

    private List<ProposalSnapshot> proposalsOf(ExpeditionId expeditionId) {
        return proposalReview.of(expeditionId, FIRST_PAGE).items();
    }

    private RecordedEvent reported(Sampling sampling) {
        return eventLog.page(
                EventFilter.forExpeditions(Set.of(sampling.expeditionId())).withTypes(Set.of("IncidentReported")), FIRST_PAGE
        ).items().getFirst();
    }

    private Delivery deliveryOf(Sampling sampling, IncidentId incidentId) {
        return replanning(incidents.incident(sampling.expeditionId(), incidentId).processing());
    }

    private static Delivery replanning(List<Delivery> processing) {
        List<Delivery> replans = processing.stream().filter(delivery -> delivery.consumer().equals(ReplanOnIncident.NAME)).toList();
        assertEquals(1, replans.size());
        return replans.getFirst();
    }

    private static List<String> consumersOf(List<Delivery> processing) {
        return processing.stream().map(Delivery::consumer).toList();
    }

    private void afterTheBackoff(Duration wait) {
        clock.set(clock.now().plus(wait));
        dispatcher.dispatchDue();
    }

    @Test
    void recordingAnIncidentAppendsItsEventButCreatesNoProposalYet() {
        Sampling sampling = runningSampling();
        int mark = eventMark();

        IncidentId incidentId = incidentOn(sampling);

        assertAll(
                () -> assertEquals(
                        List.of(new IncidentReported(sampling.expeditionId(), at(2), incidentId, Optional.of(sampling.activityId()), STORM)),
                        eventsSince(mark)
                ),
                () -> assertEquals(List.of(), proposalsOf(sampling.expeditionId()))
        );
    }

    @Test
    void dispatchingCreatesExactlyOneProposalAndOneReplanProposed() {
        Sampling sampling = runningSampling();
        IncidentId incidentId = incidentOn(sampling);
        int mark = eventMark();

        dispatcher.dispatchDue();

        ProposalSnapshot proposal = proposalsOf(sampling.expeditionId()).getFirst();
        assertAll(
                () -> assertEquals(1, proposalsOf(sampling.expeditionId()).size()),
                () -> assertEquals(incidentId, proposal.incident().id()),
                () -> assertEquals(
                        List.of(new ReplanProposed(sampling.expeditionId(), at(2), proposal.id(), proposal.suggested().id(), incidentId)),
                        eventsSince(mark)
                )
        );
    }

    @Test
    void dispatchingTwiceLeavesOneProposal() {
        Sampling sampling = runningSampling();
        incidentOn(sampling);

        dispatcher.dispatchDue();
        afterTheBackoff(Duration.ofHours(1));

        assertEquals(1, proposalsOf(sampling.expeditionId()).size());
    }

    @Test
    void deliveringTheSameEventAgainLeavesOneProposalAndOneReplanProposed() {
        Sampling sampling = runningSampling();
        incidentOn(sampling);
        dispatcher.dispatchDue();
        RecordedEvent event = reported(sampling);
        int mark = eventMark();

        replanOnIncident.consume(event);

        assertAll(
                () -> assertEquals(1, proposalsOf(sampling.expeditionId()).size()),
                () -> assertEquals(List.of(), eventsSince(mark))
        );
    }

    @Test
    void aConsumerFailureKeepsTheIncidentAndLeavesTheDeliveryRetrying() {
        Sampling sampling = runningSampling();
        respondingFails();
        IncidentId incidentId = incidentOn(sampling);

        dispatcher.dispatchDue();

        assertAll(
                () -> assertEquals(1, runs.find(sampling.expeditionId()).orElseThrow().incidents().size()),
                () -> assertEquals(List.of(), proposalsOf(sampling.expeditionId())),
                () -> assertEquals(
                        new Delivery(
                                ReplanOnIncident.NAME, DeliveryStatus.RETRYING, 1, Optional.of(at(2)),
                                Optional.of("java.lang.IllegalStateException: replan unavailable")
                        ),
                        deliveryOf(sampling, incidentId)
                )
        );
    }

    @Test
    void onceTheConsumerRecoversTheProposalIsCreatedAfterTheBackoff() {
        Sampling sampling = runningSampling();
        respondingFails();
        IncidentId incidentId = incidentOn(sampling);
        dispatcher.dispatchDue();
        respondingWorks();

        dispatcher.dispatchDue();
        boolean notBeforeTheBackoff = proposalsOf(sampling.expeditionId()).isEmpty();
        afterTheBackoff(Duration.ofSeconds(1));

        assertAll(
                () -> assertTrue(notBeforeTheBackoff),
                () -> assertEquals(1, proposalsOf(sampling.expeditionId()).size()),
                () -> assertEquals(DeliveryStatus.DELIVERED, deliveryOf(sampling, incidentId).status())
        );
    }

    @Test
    void aPermanentFailureEndsFailedAndAManualRetryDeliversIt() {
        Sampling sampling = runningSampling();
        respondingFails();
        IncidentId incidentId = incidentOn(sampling);
        dispatcher.dispatchDue();
        for (int attempt = 2; attempt <= 5; attempt++) {
            afterTheBackoff(Duration.ofMinutes(1));
        }
        Delivery failed = deliveryOf(sampling, incidentId);
        EventId eventId = reported(sampling).id();
        respondingWorks();
        afterTheBackoff(Duration.ofHours(1));
        Delivery stillFailed = deliveryOf(sampling, incidentId);

        manage.retry(eventId, ReplanOnIncident.NAME);
        Delivery pending = deliveryOf(sampling, incidentId);
        dispatcher.dispatchDue();

        assertAll(
                () -> assertEquals(DeliveryStatus.FAILED, failed.status()),
                () -> assertEquals(5, failed.attempts()),
                () -> assertEquals(failed, stillFailed),
                () -> assertEquals(DeliveryStatus.PENDING, pending.status()),
                () -> assertEquals(0, pending.attempts()),
                () -> assertEquals(failed.lastError(), pending.lastError()),
                () -> assertEquals(DeliveryStatus.DELIVERED, deliveryOf(sampling, incidentId).status()),
                () -> assertEquals(1, proposalsOf(sampling.expeditionId()).size())
        );
    }

    @Test
    void retryingADeliveryThatDidNotFailIsRejected() {
        Sampling sampling = runningSampling();
        incidentOn(sampling);
        dispatcher.dispatchDue();
        EventId eventId = reported(sampling).id();

        assertThrows(InvalidDeliveryTransition.class, () -> manage.retry(eventId, ReplanOnIncident.NAME));
    }

    @Test
    void retryingAnUnknownDeliveryNamesTheConsumer() {
        UnknownResource rejected = assertThrows(
                UnknownResource.class, () -> manage.retry(new EventId(UUID.randomUUID()), "ghost")
        );

        assertEquals("ghost", rejected.id());
    }

    @Test
    void anIncidentThatIsNotOnTheRunIsAnInvalidValue() {
        Sampling sampling = runningSampling();
        ExpeditionId run = sampling.expeditionId();
        IncidentId unknown = new IncidentId(UUID.randomUUID());

        assertThrows(InvalidValue.class, () -> responder.respondTo(run, unknown));
    }

    @Test
    void aPlanThatChangedBeforeTheDispatchEndsDeliveredWithoutAProposal() {
        TwoSamplings plan = approvedTwoSamplings();
        tracking.start(plan.expeditionId());
        ExpeditionId revision = revisionWithout(plan.expeditionId(), plan.first());
        clock.set(at(2));
        IncidentId incidentId = incidents.record(plan.expeditionId(), STORM, plan.first());
        review.submit(revision);
        approval.approve(revision);

        dispatcher.dispatchDue();

        IncidentSnapshot snapshot = incidents.incident(plan.expeditionId(), incidentId);
        assertAll(
                () -> assertEquals(Optional.empty(), snapshot.proposal()),
                () -> assertEquals(DeliveryStatus.DELIVERED, replanning(snapshot.processing()).status()),
                () -> assertEquals(List.of(), proposalsOf(plan.expeditionId())),
                () -> assertEquals(List.of(), proposalsOf(revision))
        );
    }

    @Test
    void theIncidentSnapshotBeforeTheDispatchHasNoProposalNorProcessing() {
        Sampling sampling = runningSampling();
        IncidentId incidentId = incidentOn(sampling);

        IncidentSnapshot snapshot = incidents.incident(sampling.expeditionId(), incidentId);

        assertAll(
                () -> assertEquals(incidentId, snapshot.incident().id()),
                () -> assertEquals(Optional.empty(), snapshot.proposal()),
                () -> assertEquals(List.of(), snapshot.processing())
        );
    }

    @Test
    void theIncidentSnapshotAfterTheDispatchHasItsProposalAndDelivery() {
        Sampling sampling = runningSampling();
        IncidentId incidentId = incidentOn(sampling);
        dispatcher.dispatchDue();

        IncidentSnapshot snapshot = incidents.incident(sampling.expeditionId(), incidentId);

        assertAll(
                () -> assertEquals(Optional.of(proposalsOf(sampling.expeditionId()).getFirst().id()), snapshot.proposal()),
                () -> assertEquals(
                        new Delivery(ReplanOnIncident.NAME, DeliveryStatus.DELIVERED, 1, Optional.of(at(2)), Optional.empty()),
                        replanning(snapshot.processing())
                )
        );
    }

    @Test
    void aGeneralIncidentIsOnlyNotifiedAfterTheDispatch() {
        Sampling sampling = runningSampling();
        IncidentId incidentId = incidents.record(sampling.expeditionId(), STORM);

        dispatcher.dispatchDue();

        IncidentSnapshot snapshot = incidents.incident(sampling.expeditionId(), incidentId);
        assertAll(
                () -> assertEquals(Optional.empty(), snapshot.proposal()),
                () -> assertEquals(List.of(NotifyResponsiblesConsumer.NAME), consumersOf(snapshot.processing()))
        );
    }

    @Test
    void anUnknownIncidentOrRunIsAnUnknownIncident() {
        Sampling sampling = runningSampling();
        Sampling withoutRun = approvedSampling();
        IncidentId unknown = new IncidentId(UUID.randomUUID());

        assertAll(
                () -> assertEquals(unknown.value().toString(), assertThrows(
                        UnknownResource.class, () -> incidents.incident(sampling.expeditionId(), unknown)
                ).id()),
                () -> assertEquals(unknown.value().toString(), assertThrows(
                        UnknownResource.class, () -> incidents.incident(withoutRun.expeditionId(), unknown)
                ).id())
        );
    }

    @Test
    void anIncidentBeyondTheFirstPageOfEventsIsFound() {
        Sampling sampling = runningSampling();
        for (int count = 0; count < PageRequest.MAX_SIZE; count++) {
            incidents.record(sampling.expeditionId(), STORM);
        }
        IncidentId beyond = incidentOn(sampling);
        for (int pass = 0; pass < 4; pass++) {
            dispatcher.dispatchDue();
        }

        assertEquals(
                List.of(NotifyResponsiblesConsumer.NAME, ReplanOnIncident.NAME),
                consumersOf(incidents.incident(sampling.expeditionId(), beyond).processing())
        );
    }

    @Test
    void theAuditShowsTheDeliveriesOfAnEvent() {
        Sampling sampling = runningSampling();
        incidentOn(sampling);
        dispatcher.dispatchDue();

        assertEquals(
                List.of(NotifyResponsiblesConsumer.NAME, ReplanOnIncident.NAME),
                consumersOf(audit.deliveries(reported(sampling).id()))
        );
    }

    @Test
    void aProposalKeepsTheHourOfTheDispatchNotTheOneOfTheIncident() {
        Sampling sampling = runningSampling();
        incidentOn(sampling);
        Instant dispatchedAt = at(3);
        clock.set(dispatchedAt);
        int mark = eventMark();

        dispatcher.dispatchDue();

        assertEquals(List.of(dispatchedAt), eventsSince(mark).stream().map(event -> event.occurredAt()).toList());
        assertEquals(ReplanProposal.Decision.PENDING, proposalsOf(sampling.expeditionId()).getFirst().decision());
    }
}
