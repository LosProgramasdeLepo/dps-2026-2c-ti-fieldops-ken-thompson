package edu.itba.fieldops.api.event;

import edu.itba.fieldops.domain.event.ActivityCompleted;
import edu.itba.fieldops.domain.event.ActivityStarted;
import edu.itba.fieldops.domain.event.DomainEvent;
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
import edu.itba.fieldops.domain.shared.Consumption;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.domain.tracking.IncidentDescription;
import edu.itba.fieldops.domain.tracking.Outcome;
import edu.itba.fieldops.domain.usecase.audit.ConsultEvents;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventsController.class)
class EventsControllerTest {
    private static final Instant AT = Instant.parse("2026-11-01T08:00:00Z");
    private static final Instant RECORDED_AT = Instant.parse("2026-11-01T08:00:01.000001Z");
    private static final ExpeditionId PLAN = new ExpeditionId(UUID.randomUUID());
    private static final ExpeditionId OTHER = new ExpeditionId(UUID.randomUUID());
    private static final ActivityId ACTIVITY = new ActivityId(UUID.randomUUID());
    private static final ConsumableId WATER = new ConsumableId(UUID.randomUUID());
    private static final IncidentId INCIDENT = new IncidentId(UUID.randomUUID());
    private static final ProposalId PROPOSAL = new ProposalId(UUID.randomUUID());
    private static final PersonId ADA = new PersonId(UUID.randomUUID());

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConsultEvents events;

    static Stream<DomainEvent> examples() {
        return Stream.of(
                new ExpeditionApproved(PLAN, AT, 2, Optional.of(OTHER)),
                new RunStarted(PLAN, AT),
                new ActivityStarted(PLAN, AT, ACTIVITY),
                new ActivityCompleted(PLAN, AT, ACTIVITY, AT.minusSeconds(60), new Outcome("done"), Map.of(WATER, new Consumption(2))),
                new IncidentReported(PLAN, AT, INCIDENT, Optional.of(ACTIVITY), new IncidentDescription("storm")),
                new RunFinished(PLAN, AT),
                new ReplanProposed(PLAN, AT, PROPOSAL, OTHER, INCIDENT),
                new ReplanDecided(PLAN, AT, PROPOSAL, ReplanProposal.Decision.ACCEPTED, ADA)
        );
    }

    @ParameterizedTest
    @MethodSource("examples")
    void rendersTheEnvelopeAndThePayloadOfEveryType(DomainEvent event) throws Exception {
        RecordedEvent recorded = new RecordedEvent(new EventId(UUID.randomUUID()), 5, RECORDED_AT, event);
        when(events.event(recorded.id())).thenReturn(recorded);

        ResultActions result = mvc.perform(get("/v1/events/{id}", recorded.id().value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(recorded.id().value().toString()))
                .andExpect(jsonPath("$.sequence").value(5))
                .andExpect(jsonPath("$.type").value(event.type()))
                .andExpect(jsonPath("$.expeditionId").value(PLAN.value().toString()))
                .andExpect(jsonPath("$.occurredAt").value("2026-11-01T08:00:00Z"))
                .andExpect(jsonPath("$.recordedAt").value("2026-11-01T08:00:01.000001Z"));
        switch (event) {
            case ExpeditionApproved approved -> result
                    .andExpect(jsonPath("$.payload.version").value(2))
                    .andExpect(jsonPath("$.payload.supersededId").value(OTHER.value().toString()));
            case RunStarted started -> result.andExpect(jsonPath("$.payload").isEmpty());
            case ActivityStarted started -> result.andExpect(jsonPath("$.payload.activityId").value(ACTIVITY.value().toString()));
            case ActivityCompleted completed -> result
                    .andExpect(jsonPath("$.payload.activityId").value(ACTIVITY.value().toString()))
                    .andExpect(jsonPath("$.payload.startedAt").value("2026-11-01T07:59:00Z"))
                    .andExpect(jsonPath("$.payload.result").value("done"))
                    .andExpect(jsonPath("$.payload.consumed['" + WATER.value() + "']").value(2));
            case IncidentReported reported -> result
                    .andExpect(jsonPath("$.payload.incidentId").value(INCIDENT.value().toString()))
                    .andExpect(jsonPath("$.payload.activityId").value(ACTIVITY.value().toString()))
                    .andExpect(jsonPath("$.payload.description").value("storm"));
            case RunFinished finished -> result.andExpect(jsonPath("$.payload").isEmpty());
            case ReplanProposed proposed -> result
                    .andExpect(jsonPath("$.payload.proposalId").value(PROPOSAL.value().toString()))
                    .andExpect(jsonPath("$.payload.suggestedId").value(OTHER.value().toString()))
                    .andExpect(jsonPath("$.payload.incidentId").value(INCIDENT.value().toString()));
            case ReplanDecided decided -> result
                    .andExpect(jsonPath("$.payload.proposalId").value(PROPOSAL.value().toString()))
                    .andExpect(jsonPath("$.payload.decision").value("ACCEPTED"))
                    .andExpect(jsonPath("$.payload.decidedBy").value(ADA.value().toString()));
        }
    }

    @Test
    void anOptionalFieldThatIsAbsentIsOmitted() throws Exception {
        RecordedEvent recorded = new RecordedEvent(
                new EventId(UUID.randomUUID()), 1, RECORDED_AT, new IncidentReported(PLAN, AT, INCIDENT, Optional.empty(), new IncidentDescription("storm"))
        );
        when(events.event(recorded.id())).thenReturn(recorded);

        mvc.perform(get("/v1/events/{id}", recorded.id().value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.activityId").doesNotExist());
    }

    @Test
    void pagesAPlansEventsWithLinksAndTypeFilters() throws Exception {
        PageRequest request = new PageRequest(1, 2);
        RecordedEvent recorded = new RecordedEvent(new EventId(UUID.randomUUID()), 3, RECORDED_AT, new RunStarted(PLAN, AT));
        when(events.ofExpedition(PLAN, Set.of("RunStarted", "RunFinished"), request)).thenReturn(new Page<>(List.of(recorded), request, 5));

        mvc.perform(get("/v1/expeditions/{id}/events", PLAN.value())
                        .param("page", "1").param("size", "2").param("type", "RunStarted").param("type", "RunFinished"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].type").value("RunStarted"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalItems").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(header().exists("Link"));
    }

    @Test
    void listsEveryEventWithTheDefaultPage() throws Exception {
        PageRequest request = new PageRequest(0, 20);
        when(events.all(Set.of(), request)).thenReturn(new Page<>(List.of(), request, 0));

        mvc.perform(get("/v1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(header().string("Link",
                        "<http://localhost/v1/events?page=0&size=20>; rel=\"first\", <http://localhost/v1/events?page=0&size=20>; rel=\"last\""));
    }

    @Test
    void anUnknownPlanOrEventIs404() throws Exception {
        UUID plan = UUID.randomUUID();
        UUID event = UUID.randomUUID();
        when(events.ofExpedition(new ExpeditionId(plan), Set.of(), new PageRequest(0, 20)))
                .thenThrow(new UnknownResource("expedition", plan.toString()));
        when(events.event(new EventId(event))).thenThrow(new UnknownResource("event", event.toString()));

        mvc.perform(get("/v1/expeditions/{id}/events", plan)).andExpect(status().isNotFound());
        mvc.perform(get("/v1/events/{id}", event)).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "page=first", "size=0", "size=101", "size=many", "type=Nope", "type=RunStarted&type=Nope"})
    void rejectsAnInvalidQuery(String query) throws Exception {
        mvc.perform(get("/v1/events?" + query)).andExpect(status().isBadRequest());
        mvc.perform(get("/v1/expeditions/{id}/events?" + query, UUID.randomUUID())).andExpect(status().isBadRequest());
    }

    @Test
    void anInvalidEventIdIs400() throws Exception {
        mvc.perform(get("/v1/events/not-a-uuid")).andExpect(status().isBadRequest());
    }
}
