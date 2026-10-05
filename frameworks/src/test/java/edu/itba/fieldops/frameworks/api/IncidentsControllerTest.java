package edu.itba.fieldops.frameworks.api;

import edu.itba.fieldops.api.expedition.IncidentsController;
import edu.itba.fieldops.domain.tracking.InvalidActivityExecution;
import edu.itba.fieldops.usecase.expedition.RecordIncident;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IncidentsController.class)
class IncidentsControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RecordIncident incidents;

    @Test
    void recordsAnIncidentWithAndWithoutAnActivity() throws Exception {
        UUID expeditionId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        mvc.perform(post("/v1/expeditions/{id}/incidents", expeditionId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"radio failed\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/v1/expeditions/{id}/incidents", expeditionId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"gear flooded\",\"activityId\":\"" + activityId + "\"}"))
                .andExpect(status().isNoContent());
        verify(incidents).record(any(), eq("radio failed"));
        verify(incidents).record(any(), eq("gear flooded"), any());
    }

    @Test
    void anIncidentOutsideTheRunConflicts() throws Exception {
        doThrow(new InvalidActivityExecution("no run in progress")).when(incidents).record(any(), any());

        mvc.perform(post("/v1/expeditions/{id}/incidents", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"radio failed\"}"))
                .andExpect(status().isConflict());
    }
}
