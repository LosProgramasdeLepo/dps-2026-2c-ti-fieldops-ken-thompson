package edu.itba.fieldops.frameworks.api;

import edu.itba.fieldops.api.expedition.RunController;
import edu.itba.fieldops.domain.expedition.InvalidExpeditionTransition;
import edu.itba.fieldops.domain.expedition.ExpeditionStatus;
import edu.itba.fieldops.usecase.expedition.TrackExpedition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RunController.class)
class RunControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TrackExpedition tracking;

    @Test
    void tracksTheRun() throws Exception {
        UUID id = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        mvc.perform(post("/v1/expeditions/{id}/run", id)).andExpect(status().isNoContent());
        mvc.perform(post("/v1/expeditions/{id}/run/suspension", id)).andExpect(status().isNoContent());
        mvc.perform(delete("/v1/expeditions/{id}/run/suspension", id)).andExpect(status().isNoContent());
        mvc.perform(post("/v1/expeditions/{id}/run/activities", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activityId\":\"" + activityId + "\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/v1/expeditions/{id}/run/activities/{activityId}/completion", id, activityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"sample stored\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/v1/expeditions/{id}/run/observations", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"water level rose\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/v1/expeditions/{id}/run/completion", id)).andExpect(status().isNoContent());
    }

    @Test
    void startingADraftConflicts() throws Exception {
        doThrow(new InvalidExpeditionTransition(ExpeditionStatus.DRAFT, "start")).when(tracking).start(any());

        mvc.perform(post("/v1/expeditions/{id}/run", UUID.randomUUID()))
                .andExpect(status().isConflict());
    }
}
