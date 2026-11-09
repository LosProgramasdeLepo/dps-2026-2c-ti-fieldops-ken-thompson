package edu.itba.fieldops.api.expedition;

import edu.itba.fieldops.domain.event.Delivery;
import edu.itba.fieldops.domain.event.DeliveryStatus;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.IncidentId;
import edu.itba.fieldops.domain.identity.ProposalId;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.domain.tracking.IncidentDescription;
import edu.itba.fieldops.domain.usecase.expedition.IncidentSnapshot;
import edu.itba.fieldops.domain.usecase.expedition.RecordIncident;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IncidentsController.class)
class IncidentsControllerTest {
    private static final Instant AT = Instant.parse("2026-11-01T08:00:00Z");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RecordIncident incidents;

    @Test
    void recordsAnIncidentAndPointsToIt() throws Exception {
        UUID expeditionId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        when(incidents.record(new ExpeditionId(expeditionId), new IncidentDescription("radio failed")))
                .thenReturn(new IncidentId(incidentId));

        mvc.perform(post("/v1/expeditions/{id}/incidents", expeditionId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"radio failed\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/v1/expeditions/" + expeditionId + "/incidents/" + incidentId))
                .andExpect(jsonPath("$.id").value(incidentId.toString()));
    }

    @Test
    void recordsAnIncidentAgainstAnActivity() throws Exception {
        UUID expeditionId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        when(incidents.record(new ExpeditionId(expeditionId), new IncidentDescription("gear flooded"), new ActivityId(activityId)))
                .thenReturn(new IncidentId(incidentId));

        mvc.perform(post("/v1/expeditions/{id}/incidents", expeditionId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"gear flooded\",\"activityId\":\"" + activityId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/v1/expeditions/" + expeditionId + "/incidents/" + incidentId))
                .andExpect(jsonPath("$.id").value(incidentId.toString()));
    }

    @Test
    void showsAnIncidentWithItsProposalAndProcessing() throws Exception {
        UUID expeditionId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        UUID proposalId = UUID.randomUUID();
        when(incidents.incident(new ExpeditionId(expeditionId), new IncidentId(incidentId))).thenReturn(new IncidentSnapshot(
                new Incident(new IncidentId(incidentId), new IncidentDescription("storm"), AT, Optional.of(new ActivityId(activityId))),
                Optional.of(new ProposalId(proposalId)),
                List.of(new Delivery("replan-on-incident", DeliveryStatus.DELIVERED, 1, Optional.of(AT), Optional.empty()))
        ));

        mvc.perform(get("/v1/expeditions/{id}/incidents/{incidentId}", expeditionId, incidentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(incidentId.toString()))
                .andExpect(jsonPath("$.description").value("storm"))
                .andExpect(jsonPath("$.at").value("2026-11-01T08:00:00Z"))
                .andExpect(jsonPath("$.activityId").value(activityId.toString()))
                .andExpect(jsonPath("$.proposalId").value(proposalId.toString()))
                .andExpect(jsonPath("$.processing[0].consumer").value("replan-on-incident"))
                .andExpect(jsonPath("$.processing[0].status").value("DELIVERED"))
                .andExpect(jsonPath("$.processing[0].attempts").value(1))
                .andExpect(jsonPath("$.processing[0].lastAttemptAt").value("2026-11-01T08:00:00Z"))
                .andExpect(jsonPath("$.processing[0].lastError").doesNotExist());
    }

    @Test
    void anIncidentNotYetProcessedOmitsTheProposalAndHasNoDeliveries() throws Exception {
        UUID expeditionId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        when(incidents.incident(new ExpeditionId(expeditionId), new IncidentId(incidentId))).thenReturn(new IncidentSnapshot(
                new Incident(new IncidentId(incidentId), new IncidentDescription("storm"), AT, Optional.empty()),
                Optional.empty(),
                List.of()
        ));

        mvc.perform(get("/v1/expeditions/{id}/incidents/{incidentId}", expeditionId, incidentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activityId").doesNotExist())
                .andExpect(jsonPath("$.proposalId").doesNotExist())
                .andExpect(jsonPath("$.processing").isArray())
                .andExpect(jsonPath("$.processing").isEmpty());
    }

    @Test
    void anUnknownIncidentIs404() throws Exception {
        UUID expeditionId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        when(incidents.incident(new ExpeditionId(expeditionId), new IncidentId(incidentId)))
                .thenThrow(new UnknownResource("incident", incidentId.toString()));

        mvc.perform(get("/v1/expeditions/{id}/incidents/{incidentId}", expeditionId, incidentId))
                .andExpect(status().isNotFound());
    }
}
