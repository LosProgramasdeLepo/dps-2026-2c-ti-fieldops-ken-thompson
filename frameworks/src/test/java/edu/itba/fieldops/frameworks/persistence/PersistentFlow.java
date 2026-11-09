package edu.itba.fieldops.frameworks.persistence;

import edu.itba.fieldops.domain.event.EventLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.cloud.config.enabled=false")
@AutoConfigureMockMvc
abstract class PersistentFlow {
    @Autowired
    private MockMvc mvc;

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
        noContent(post("/v1/expeditions/{id}/incidents", expeditionId), """
                {"description":"Flooded trail","activityId":"%s"}
                """.formatted(activityId));

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
                .andExpect(jsonPath("$.items[4].payload.decision").value("ACCEPTED"));
        MvcResult listed = mvc.perform(get("/v1/events").param("type", "ReplanDecided"))
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
        mvc.perform(get("/v1/events").param("type", "ExpeditionApproved"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    private record PlanInReview(UUID personId, UUID expeditionId, UUID activityId) {
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
