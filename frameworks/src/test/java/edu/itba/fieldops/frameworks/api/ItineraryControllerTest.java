package edu.itba.fieldops.frameworks.api;

import edu.itba.fieldops.api.expedition.ItineraryController;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.InvalidItinerary;
import edu.itba.fieldops.usecase.expedition.PlanItinerary;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItineraryController.class)
class ItineraryControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PlanItinerary itinerary;

    @Test
    void addsASamplingActivityAndReturnsItsId() throws Exception {
        UUID expeditionId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        UUID certificationId = UUID.randomUUID();
        when(itinerary.nextActivityId()).thenReturn(new ActivityId(activityId));

        mvc.perform(post("/v1/expeditions/{id}/activities", expeditionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sampling(certificationId)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/v1/expeditions/" + expeditionId + "/activities/" + activityId))
                .andExpect(jsonPath("$.id").value(activityId.toString()));

        ArgumentCaptor<Activity> activity = ArgumentCaptor.forClass(Activity.class);
        verify(itinerary).addActivity(any(), activity.capture());
        assertEquals("Soil sampling", activity.getValue().name());
        assertEquals(new CertificationId(certificationId), activity.getValue().requirements().certifications().iterator().next());
    }

    @Test
    void addsASequentialBlock() throws Exception {
        UUID expeditionId = UUID.randomUUID();
        when(itinerary.nextActivityId()).thenReturn(new ActivityId(UUID.randomUUID()), new ActivityId(UUID.randomUUID()));

        mvc.perform(post("/v1/expeditions/{id}/blocks", expeditionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"arrangement":"SEQUENTIAL","parts":[
                                  {"node":"ACTIVITY","activity":%s},
                                  {"node":"ACTIVITY","activity":%s}
                                ]}
                                """.formatted(transit(), transit())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.arrangement").value("SEQUENTIAL"))
                .andExpect(jsonPath("$.parts.length()").value(2))
                .andExpect(jsonPath("$.parts[0].node").value("ACTIVITY"));
    }

    @Test
    void anIllegalWindowIsUnprocessable() throws Exception {
        when(itinerary.nextActivityId()).thenReturn(new ActivityId(UUID.randomUUID()));
        doThrow(new InvalidItinerary("activity window is shorter than estimated duration"))
                .when(itinerary).addActivity(any(), any());

        mvc.perform(post("/v1/expeditions/{id}/activities", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sampling(UUID.randomUUID())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void addsAPredecessor() throws Exception {
        mvc.perform(put(
                        "/v1/expeditions/{id}/activities/{activityId}/predecessors/{predecessorId}",
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID()
                ))
                .andExpect(status().isNoContent());
    }

    private static String sampling(UUID certificationId) {
        return """
                {"kind":"SAMPLING","certification":"%s","name":"Soil sampling","estimatedDuration":"PT4H","risk":"MEDIUM","consumption":{},"zone":"Delta","window":{"start":"2026-11-01T08:00:00Z","end":"2026-11-01T12:00:00Z"},"predecessors":[]}
                """.formatted(certificationId);
    }

    private static String transit() {
        return """
                {"kind":"TRANSIT","name":"Crossing","estimatedDuration":"PT2H","risk":"LOW","consumption":{},"zone":"Delta","window":{"start":"2026-11-01T08:00:00Z","end":"2026-11-01T10:00:00Z"},"predecessors":[]}
                """;
    }
}
