package edu.itba.fieldops.frameworks.persistence;

import edu.itba.fieldops.adapters.messaging.EventConsumer;
import edu.itba.fieldops.domain.event.DomainEvent;
import edu.itba.fieldops.domain.event.EventLog;
import edu.itba.fieldops.domain.event.ExpeditionApproved;
import edu.itba.fieldops.domain.event.RecordedEvent;
import edu.itba.fieldops.domain.expedition.ExpeditionCharter;
import edu.itba.fieldops.domain.expedition.Objective;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.usecase.expedition.DraftExpedition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "fieldops.events.dispatch-delay=50ms",
        "fieldops.events.max-attempts=2",
        "fieldops.events.backoff-base=1s",
        "fieldops.events.backoff-max=1s"
})
@AutoConfigureMockMvc
abstract class PersistentFlow {
    private static final Duration PATIENCE = Duration.ofSeconds(10);
    private static final String REPLAN = "replan-on-incident";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private FailsAfterWriting failing;

    @MockitoSpyBean
    private EventLog eventLog;

    private PlanInReview planInReview() throws Exception {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        String period = period(now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(4)));
        UUID certificationId = created(post("/v1/certifications"), "{\"name\":\"Sampling\"}");
        UUID personId = created(post("/v1/people"), """
                {"name":"Ada","certifications":["%s"],"availability":{"periods":[%s]}}
                """.formatted(certificationId, period));
        UUID permitId = created(post("/v1/permits"), """
                {"kind":"zone","zone":"Delta","validity":%s}
                """.formatted(period));
        UUID expeditionId = created(post("/v1/expeditions"), """
                {"objectives":["Map wetland"],"period":%s,"zones":["Delta"],"responsibles":["%s"],"restrictions":[]}
                """.formatted(period, personId));
        UUID activityId = created(post("/v1/expeditions/{id}/activities", expeditionId), """
                {"kind":"SAMPLING","certification":"%s","name":"Soil sampling","estimatedDuration":"PT4H","risk":"MEDIUM","consumption":{},"zone":"Delta","window":%s,"predecessors":[]}
                """.formatted(certificationId, period(now.minus(Duration.ofHours(1)), now.plus(Duration.ofHours(3)))));
        noContent(post("/v1/expeditions/{id}/assignments", expeditionId), """
                {"type":"PERSON","activityId":"%s","personId":"%s"}
                """.formatted(activityId, personId));
        mvc.perform(put("/v1/expeditions/{id}/permits/{permitId}", expeditionId, permitId)).andExpect(status().isNoContent());
        mvc.perform(post("/v1/expeditions/{id}/submission", expeditionId)).andExpect(status().isNoContent());
        return new PlanInReview(personId, expeditionId, activityId);
    }

    @Test
    void plansApprovesRunsAndReplansAnExpedition() throws Exception {
        PlanInReview plan = planInReview();
        UUID personId = plan.personId();
        UUID expeditionId = plan.expeditionId();
        UUID activityId = plan.activityId();
        mvc.perform(post("/v1/expeditions/{id}/approval", expeditionId)).andExpect(status().isNoContent());
        mvc.perform(post("/v1/expeditions/{id}/run", expeditionId)).andExpect(status().isNoContent());
        UUID incidentId = created(post("/v1/expeditions/{id}/incidents", expeditionId), """
                {"description":"Flooded trail","activityId":"%s"}
                """.formatted(activityId));
        await().atMost(PATIENCE).untilAsserted(() -> mvc.perform(
                        get("/v1/expeditions/{id}/incidents/{incidentId}", expeditionId, incidentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proposalId", notNullValue()))
                .andExpect(jsonPath("$.processing[0].consumer").value(REPLAN))
                .andExpect(jsonPath("$.processing[0].status").value("DELIVERED")));

        MvcResult proposals = mvc.perform(get("/v1/expeditions/{id}/replan-proposals", expeditionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].decision").value("PENDING"))
                .andExpect(jsonPath("$.items[0].incident.description").value("Flooded trail"))
                .andReturn();
        UUID proposalId = id(proposals);
        noContent(post("/v1/replan-proposals/{id}/acceptance", proposalId), """
                {"responsible":"%s"}
                """.formatted(personId));

        mvc.perform(get("/v1/replan-proposals/{id}", proposalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("ACCEPTED"))
                .andExpect(jsonPath("$.decidedBy").value(personId.toString()))
                .andExpect(jsonPath("$.originalId").value(expeditionId.toString()));
        mvc.perform(get("/v1/expeditions/{id}", expeditionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        mvc.perform(get("/v1/expeditions/{id}/run", expeditionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidents[0].description").value("Flooded trail"));
        mvc.perform(get("/v1/expeditions/{id}/events", expeditionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(5))
                .andExpect(jsonPath("$.items[*].type", contains(
                        "ExpeditionApproved", "RunStarted", "IncidentReported", "ReplanProposed", "ReplanDecided"
                )))
                .andExpect(jsonPath("$.items[0].payload.version").value(1))
                .andExpect(jsonPath("$.items[2].payload.description").value("Flooded trail"))
                .andExpect(jsonPath("$.items[4].payload.decision").value("ACCEPTED"))
                .andExpect(jsonPath("$.items[2].deliveries[0].status").value("DELIVERED"));
        MvcResult listed = mvc.perform(get("/v1/expeditions/{id}/events", expeditionId).param("type", "ReplanDecided"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andReturn();
        mvc.perform(get("/v1/events/{id}", id(listed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("ReplanDecided"))
                .andExpect(jsonPath("$.payload.decidedBy").value(personId.toString()));
    }

    @Test
    void anApprovalThatFailsAfterAppendingItsEventLeavesNeitherTheEventNorTheApproval() throws Exception {
        PlanInReview plan = planInReview();
        AtomicBoolean appended = new AtomicBoolean();
        doAnswer(invocation -> {
            invocation.callRealMethod();
            appended.set(true);
            throw new IllegalStateException("fails after the event was appended");
        }).when(AopTestUtils.<EventLog>getUltimateTargetObject(eventLog)).append(any());

        mvc.perform(post("/v1/expeditions/{id}/approval", plan.expeditionId())).andExpect(status().isInternalServerError());

        assertTrue(appended.get(), "the event must have been written before the failure");
        mvc.perform(get("/v1/expeditions/{id}", plan.expeditionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_REVIEW"));
        mvc.perform(get("/v1/expeditions/{id}/events", plan.expeditionId()).param("type", "ExpeditionApproved"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void aConsumerThatFailsAfterWritingLeavesNoWriteAndCanBeRetriedByHand() throws Exception {
        PlanInReview plan = planInReview();
        failing.arm(new ExpeditionId(plan.expeditionId()), new PersonId(plan.personId()));

        mvc.perform(post("/v1/expeditions/{id}/approval", plan.expeditionId())).andExpect(status().isNoContent());

        mvc.perform(get("/v1/expeditions/{id}", plan.expeditionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        UUID eventId = id(mvc.perform(get("/v1/expeditions/{id}/events", plan.expeditionId()).param("type", "ExpeditionApproved"))
                .andExpect(status().isOk())
                .andReturn());
        awaitDelivery(eventId, "RETRYING");
        mvc.perform(get("/v1/events/{id}", eventId))
                .andExpect(jsonPath(FAILING_DELIVERY + ".lastError", contains(containsString("fails after writing"))));
        assertAbsent(failing.lastDraft());
        awaitDelivery(eventId, "FAILED");
        assertAbsent(failing.lastDraft());
        mvc.perform(post("/v1/events/{id}/deliveries/{consumer}/retry", eventId, REPLAN)).andExpect(status().isNotFound());

        failing.recover();
        mvc.perform(post("/v1/events/{id}/deliveries/{consumer}/retry", eventId, FailsAfterWriting.NAME))
                .andExpect(status().isNoContent());

        awaitDelivery(eventId, "DELIVERED");
        mvc.perform(get("/v1/expeditions/{id}", failing.lastDraft())).andExpect(status().isOk());
        mvc.perform(post("/v1/events/{id}/deliveries/{consumer}/retry", eventId, FailsAfterWriting.NAME))
                .andExpect(status().isConflict());
    }

    private void awaitDelivery(UUID eventId, String expected) {
        await().atMost(PATIENCE).pollInterval(Duration.ofMillis(25)).untilAsserted(() -> mvc.perform(get("/v1/events/{id}", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath(FAILING_DELIVERY + ".status", contains(expected))));
    }

    private void assertAbsent(UUID draftId) throws Exception {
        mvc.perform(get("/v1/expeditions/{id}", draftId)).andExpect(status().isNotFound());
    }

    private static final String FAILING_DELIVERY = "$.deliveries[?(@.consumer=='" + FailsAfterWriting.NAME + "')]";

    private record PlanInReview(UUID personId, UUID expeditionId, UUID activityId) {
    }

    private record Target(ExpeditionId plan, PersonId responsible) {
    }

    static final class FailsAfterWriting implements EventConsumer {
        static final String NAME = "fails-after-writing";

        private final DraftExpedition drafts;
        private final AtomicReference<Target> target = new AtomicReference<>();
        private final AtomicReference<UUID> lastDraft = new AtomicReference<>();
        private final AtomicBoolean recovered = new AtomicBoolean();

        FailsAfterWriting(DraftExpedition drafts) {
            this.drafts = drafts;
        }

        void arm(ExpeditionId plan, PersonId responsible) {
            target.set(new Target(plan, responsible));
        }

        void recover() {
            recovered.set(true);
        }

        UUID lastDraft() {
            return lastDraft.get();
        }

        @Override
        public String name() {
            return NAME;
        }

        @Override
        public boolean subscribesTo(DomainEvent event) {
            return event instanceof ExpeditionApproved
                    && Optional.ofNullable(target.get()).filter(armed -> armed.plan().equals(event.expeditionId())).isPresent();
        }

        @Override
        public void consume(RecordedEvent event) {
            Instant now = Instant.now();
            lastDraft.set(drafts.draft(new ExpeditionCharter(
                    List.of(new Objective("Written by a consumer")),
                    new TimePeriod(now, now.plus(Duration.ofDays(1))),
                    List.of(new WorkZone("Delta")),
                    List.of(target.get().responsible()),
                    List.of()
            )).value());
            if (!recovered.get()) {
                throw new IllegalStateException("fails after writing");
            }
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FailingConsumer {
        @Bean
        FailsAfterWriting failsAfterWriting(DraftExpedition drafts) {
            return new FailsAfterWriting(drafts);
        }
    }

    private UUID created(
            MockHttpServletRequestBuilder request,
            String body
    ) throws Exception {
        return id(mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private void noContent(
            MockHttpServletRequestBuilder request,
            String body
    ) throws Exception {
        mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isNoContent());
    }

    private static String period(Instant start, Instant end) {
        return "{\"start\":\"%s\",\"end\":\"%s\"}".formatted(start, end);
    }

    private static UUID id(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        return UUID.fromString(body.replaceFirst("(?s)^.*?\"id\"\\s*:\\s*\"([^\"]+)\".*$", "$1"));
    }
}
