package edu.itba.fieldops.frameworks;

import edu.itba.fieldops.domain.event.RecordedEvent;
import edu.itba.fieldops.domain.identity.EventId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.shared.PageRequest;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.domain.tracking.IncidentDescription;
import edu.itba.fieldops.domain.usecase.audit.ConsultEvents;
import edu.itba.fieldops.domain.usecase.expedition.ProposalSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditUseCasesTest extends UseCaseFixture {
    @Autowired
    private ConsultEvents audit;

    @Test
    void aPlansHistoryHoldsTheOriginalTheRevisionTheIncidentTheProposalAndTheDecision() {
        Sampling sampling = runningSampling();
        clock.set(at(2));
        incidents.record(sampling.expeditionId(), new IncidentDescription("storm"), sampling.activityId());
        dispatcher.dispatchDue();
        ProposalSnapshot proposal = proposalReview.of(sampling.expeditionId(), FIRST_PAGE).items().getFirst();
        ExpeditionId revision = proposal.suggested().id();
        clock.set(at(3));
        proposalReview.accept(proposal.id(), sampling.responsible());
        review.submit(revision);
        approval.approve(revision);

        List<RecordedEvent> history = audit.ofExpedition(sampling.expeditionId(), Set.of(), FIRST_PAGE).items();

        assertAll(
                () -> assertEquals(
                        List.of("ExpeditionApproved", "RunStarted", "IncidentReported", "ReplanProposed", "ReplanDecided", "ExpeditionApproved"),
                        history.stream().map(item -> item.event().type()).toList()
                ),
                () -> assertTrue(history.stream().anyMatch(item -> item.event().expeditionId().equals(revision))),
                () -> assertEquals(history.stream().map(RecordedEvent::sequence).sorted().toList(), history.stream().map(RecordedEvent::sequence).toList())
        );
    }

    @Test
    void consultingFromTheRevisionReturnsTheSameFamily() {
        Sampling sampling = runningSampling();
        incidents.record(sampling.expeditionId(), new IncidentDescription("storm"), sampling.activityId());
        dispatcher.dispatchDue();
        ExpeditionId revision = proposalReview.of(sampling.expeditionId(), FIRST_PAGE).items().getFirst().suggested().id();
        proposalReview.accept(proposalReview.of(sampling.expeditionId(), FIRST_PAGE).items().getFirst().id(), sampling.responsible());

        assertEquals(
                audit.ofExpedition(sampling.expeditionId(), Set.of(), FIRST_PAGE).items(),
                audit.ofExpedition(revision, Set.of(), FIRST_PAGE).items()
        );
    }

    @Test
    void filtersByTypeAndPages() {
        Sampling sampling = runningSampling();

        assertAll(
                () -> assertEquals(
                        List.of("RunStarted"),
                        audit.ofExpedition(sampling.expeditionId(), Set.of("RunStarted"), FIRST_PAGE).items().stream()
                                .map(item -> item.event().type()).toList()
                ),
                () -> assertEquals(1, audit.all(Set.of(), new PageRequest(0, 1)).items().size()),
                () -> assertTrue(audit.all(Set.of(), new PageRequest(0, 1)).totalItems() >= 2)
        );
    }

    @Test
    void findsAnEventById() {
        runningSampling();
        RecordedEvent first = audit.all(Set.of(), FIRST_PAGE).items().getFirst();

        assertEquals(first, audit.event(first.id()));
    }

    @Test
    void anUnknownPlanOrEventIsUnknown() {
        assertAll(
                () -> assertThrows(UnknownResource.class, () -> audit.ofExpedition(new ExpeditionId(UUID.randomUUID()), Set.of(), FIRST_PAGE)),
                () -> assertThrows(UnknownResource.class, () -> audit.event(new EventId(UUID.randomUUID())))
        );
    }
}
