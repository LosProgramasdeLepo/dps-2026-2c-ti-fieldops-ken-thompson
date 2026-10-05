package edu.itba.fieldops.frameworks.api;

import edu.itba.fieldops.api.expedition.ReplanController;
import edu.itba.fieldops.domain.expedition.ExpeditionCharter;
import edu.itba.fieldops.domain.expedition.ExpeditionStatus;
import edu.itba.fieldops.domain.expedition.Objective;
import edu.itba.fieldops.domain.expedition.ReplanProposal;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.ProposalId;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.usecase.expedition.PlanSnapshot;
import edu.itba.fieldops.usecase.expedition.ProposalSnapshot;
import edu.itba.fieldops.usecase.expedition.ReplanExpedition;
import edu.itba.fieldops.usecase.expedition.ReviewReplanProposal;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReplanController.class)
class ReplanControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ReplanExpedition replan;

    @MockitoBean
    private ReviewReplanProposal proposals;

    @Test
    void opensARevision() throws Exception {
        UUID original = UUID.randomUUID();
        UUID revision = UUID.randomUUID();
        when(replan.revise(new ExpeditionId(original))).thenReturn(new ExpeditionId(revision));

        mvc.perform(post("/v1/expeditions/{id}/revisions", original))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/v1/expeditions/" + revision))
                .andExpect(jsonPath("$.id").value(revision.toString()));
    }

    @Test
    void changesADraftRevision() throws Exception {
        UUID id = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        mvc.perform(delete("/v1/expeditions/{id}/activities/{activityId}", id, activityId)).andExpect(status().isNoContent());
        mvc.perform(post("/v1/expeditions/{id}/activities/{activityId}/delay", id, activityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"delay\":\"PT1H\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/v1/expeditions/{id}/reassignments", id)).andExpect(status().isNoContent());
    }

    @Test
    void listsProposalsAndDecidesThem() throws Exception {
        UUID expeditionId = UUID.randomUUID();
        UUID proposalId = UUID.randomUUID();
        UUID suggestedId = UUID.randomUUID();
        UUID responsible = UUID.randomUUID();
        when(proposals.of(new ExpeditionId(expeditionId))).thenReturn(List.of(new ProposalSnapshot(
                new ProposalId(proposalId),
                new ExpeditionId(expeditionId),
                Incident.affecting(new ActivityId(UUID.randomUUID()), "gear flooded", Instant.parse("2026-11-01T09:00:00Z")),
                ReplanProposal.Decision.PENDING,
                Optional.empty(),
                Optional.empty(),
                plan(suggestedId, responsible)
        )));

        mvc.perform(get("/v1/expeditions/{id}/replan-proposals", expeditionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(proposalId.toString()))
                .andExpect(jsonPath("$[0].decision").value("PENDING"))
                .andExpect(jsonPath("$[0].suggested.id").value(suggestedId.toString()))
                .andExpect(jsonPath("$[0].decidedBy").doesNotExist());

        mvc.perform(post("/v1/replan-proposals/{proposalId}/acceptance", proposalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responsible\":\"" + responsible + "\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/v1/replan-proposals/{proposalId}/rejection", proposalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responsible\":\"" + responsible + "\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void aDecidedProposalIsUnprocessable() throws Exception {
        doThrow(new InvalidValue("proposal already decided")).when(proposals).accept(any(), any());

        mvc.perform(post("/v1/replan-proposals/{proposalId}/acceptance", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responsible\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    private static PlanSnapshot plan(UUID id, UUID responsible) {
        return new PlanSnapshot(
                new ExpeditionId(id),
                2,
                Optional.of(new ExpeditionId(UUID.randomUUID())),
                ExpeditionStatus.DRAFT,
                new ExpeditionCharter(
                        List.of(new Objective("Map wetland")),
                        new TimePeriod(Instant.parse("2026-11-01T00:00:00Z"), Instant.parse("2026-11-06T00:00:00Z")),
                        List.of(new WorkZone("Delta")),
                        List.of(new PersonId(responsible)),
                        List.of()
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }
}
