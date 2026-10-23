package edu.itba.fieldops.frameworks.api;

import edu.itba.fieldops.api.expedition.IncidentsController;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.tracking.InvalidActivityExecution;
import edu.itba.fieldops.domain.usecase.expedition.RecordIncident;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IncidentsController.class)
class IncidentsControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RecordIncident incidents;

    @Test
    void recordsAnIncident() throws Exception {
        UUID expeditionId = UUID.randomUUID();
        mvc.perform(post("/v1/expeditions/{id}/incidents", expeditionId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"radio failed\"}"))
                .andExpect(status().isNoContent());
        verify(incidents).record(new ExpeditionId(expeditionId), "radio failed");
    }

    @Test
    void recordsAnIncidentAgainstAnActivity() throws Exception {
        UUID expeditionId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        mvc.perform(post("/v1/expeditions/{id}/incidents", expeditionId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"gear flooded\",\"activityId\":\"" + activityId + "\"}"))
                .andExpect(status().isNoContent());
        verify(incidents).record(new ExpeditionId(expeditionId), "gear flooded", new ActivityId(activityId));
    }

    @Test
    void anIncidentOutsideTheRunConflicts() throws Exception {
        doThrow(new InvalidActivityExecution("no run in progress")).when(incidents).record(any(), any());

        mvc.perform(post("/v1/expeditions/{id}/incidents", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"radio failed\"}"))
                .andExpect(status().isConflict());
    }
}
