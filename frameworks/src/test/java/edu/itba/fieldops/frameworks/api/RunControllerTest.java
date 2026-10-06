package edu.itba.fieldops.frameworks.api;

import edu.itba.fieldops.api.expedition.RunController;
import edu.itba.fieldops.domain.expedition.ExpeditionExecution;
import edu.itba.fieldops.domain.expedition.InvalidExpeditionTransition;
import edu.itba.fieldops.domain.expedition.ExpeditionStatus;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.domain.tracking.ActivityExecution;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.usecase.expedition.RunSnapshot;
import edu.itba.fieldops.usecase.expedition.TrackExpedition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
    void readsTheRunWithItsActivitiesAndIncidents() throws Exception {
        UUID id = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        Instant start = Instant.parse("2026-11-01T08:00:00Z");
        when(tracking.run(new ExpeditionId(id))).thenReturn(new RunSnapshot(
                new ExpeditionId(id),
                new ExpeditionId(id),
                ExpeditionExecution.Status.IN_PROGRESS,
                List.of(new ActivityExecution(new ActivityId(activityId), start)),
                List.of(Incident.of("storm on site", start)),
                List.of()
        ));

        mvc.perform(get("/v1/expeditions/{id}/run", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.inForceId").value(id.toString()))
                .andExpect(jsonPath("$.activities[0].activityId").value(activityId.toString()))
                .andExpect(jsonPath("$.activities[0].startedAt").value("2026-11-01T08:00:00Z"))
                .andExpect(jsonPath("$.activities[0].finishedAt").doesNotExist())
                .andExpect(jsonPath("$.incidents[0].description").value("storm on site"));
    }

    @Test
    void aPlanThatNeverStartedHasNoRun() throws Exception {
        UUID id = UUID.randomUUID();
        when(tracking.run(new ExpeditionId(id))).thenThrow(new UnknownResource("run", id.toString()));

        mvc.perform(get("/v1/expeditions/{id}/run", id)).andExpect(status().isNotFound());
    }

    @Test
    void startingADraftConflicts() throws Exception {
        doThrow(new InvalidExpeditionTransition(ExpeditionStatus.DRAFT, "start")).when(tracking).start(any());

        mvc.perform(post("/v1/expeditions/{id}/run", UUID.randomUUID()))
                .andExpect(status().isConflict());
    }
}
