package edu.itba.fieldops.frameworks.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresContainerConfiguration.class)
class PersistentFlowTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void plansApprovesRunsAndReplansAnExpeditionStoredInPostgres() throws Exception {
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
    }

    private UUID created(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
            String body
    ) throws Exception {
        return id(mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private void noContent(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
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
