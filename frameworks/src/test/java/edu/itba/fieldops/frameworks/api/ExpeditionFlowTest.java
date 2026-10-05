package edu.itba.fieldops.frameworks.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ExpeditionFlowTest {
    private static final String PERIOD = "{\"start\":\"2026-11-01T00:00:00Z\",\"end\":\"2026-11-06T00:00:00Z\"}";

    @Autowired
    private MockMvc mvc;

    @Test
    void plansReviewsAndApprovesAnExpedition() throws Exception {
        UUID certificationId = id(mvc.perform(post("/v1/certifications").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Sampling\"}")).andExpect(status().isCreated()).andReturn());
        UUID personId = id(mvc.perform(post("/v1/people").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Ada","certifications":["%s"],"availability":{"periods":[%s]}}
                """.formatted(certificationId, PERIOD))).andExpect(status().isCreated()).andReturn());
        UUID permitId = id(mvc.perform(post("/v1/permits").contentType(MediaType.APPLICATION_JSON).content("""
                {"kind":"zone","zone":"Delta","validity":%s}
                """.formatted(PERIOD))).andExpect(status().isCreated()).andReturn());
        UUID expeditionId = id(mvc.perform(post("/v1/expeditions").contentType(MediaType.APPLICATION_JSON)
                .content(ExpeditionsControllerTest.charter(personId))).andExpect(status().isCreated()).andReturn());
        UUID activityId = id(mvc.perform(post("/v1/expeditions/{id}/activities", expeditionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"kind":"SAMPLING","certification":"%s","name":"Soil sampling","estimatedDuration":"PT4H","risk":"MEDIUM","consumption":{},"zone":"Delta","window":{"start":"2026-11-01T08:00:00Z","end":"2026-11-01T12:00:00Z"},"predecessors":[]}
                        """.formatted(certificationId))).andExpect(status().isCreated()).andReturn());
        mvc.perform(post("/v1/expeditions/{id}/assignments", expeditionId).contentType(MediaType.APPLICATION_JSON).content("""
                {"type":"PERSON","activityId":"%s","personId":"%s"}
                """.formatted(activityId, personId))).andExpect(status().isNoContent());
        mvc.perform(put("/v1/expeditions/{id}/permits/{permitId}", expeditionId, permitId)).andExpect(status().isNoContent());
        mvc.perform(post("/v1/expeditions/{id}/submission", expeditionId)).andExpect(status().isNoContent());
        mvc.perform(post("/v1/expeditions/{id}/approval", expeditionId)).andExpect(status().isNoContent());

        mvc.perform(get("/v1/expeditions/{id}", expeditionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.itinerary[0].name").value("Soil sampling"))
                .andExpect(jsonPath("$.itinerary[0].node").value("ACTIVITY"));
        mvc.perform(get("/v1/expeditions/{id}/estimate", expeditionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duration").value("PT4H"));
        mvc.perform(get("/v1/expeditions/{id}/report", expeditionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void anUnknownExpeditionIsNotFoundAndAnUnknownResponsibleIsUnprocessable() throws Exception {
        mvc.perform(get("/v1/expeditions/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
        mvc.perform(post("/v1/expeditions").contentType(MediaType.APPLICATION_JSON)
                        .content(ExpeditionsControllerTest.charter(UUID.randomUUID())))
                .andExpect(status().isUnprocessableContent());
    }

    private static UUID id(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        String value = body.replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
        return UUID.fromString(value);
    }
}
