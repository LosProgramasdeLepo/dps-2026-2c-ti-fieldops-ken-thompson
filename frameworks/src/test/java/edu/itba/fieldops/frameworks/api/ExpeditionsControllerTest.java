package edu.itba.fieldops.frameworks.api;

import edu.itba.fieldops.api.expedition.ExpeditionsController;
import edu.itba.fieldops.domain.expedition.ExpeditionCharter;
import edu.itba.fieldops.domain.expedition.ExpeditionStatus;
import edu.itba.fieldops.domain.expedition.Objective;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.usecase.expedition.ConsultExpedition;
import edu.itba.fieldops.usecase.expedition.DraftExpedition;
import edu.itba.fieldops.usecase.expedition.PlanSnapshot;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExpeditionsController.class)
class ExpeditionsControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DraftExpedition drafts;

    @MockitoBean
    private ConsultExpedition consult;

    @Test
    void draftsAnExpedition() throws Exception {
        UUID id = UUID.randomUUID();
        UUID responsible = UUID.randomUUID();
        when(drafts.draft(any())).thenReturn(new ExpeditionId(id));

        mvc.perform(post("/v1/expeditions").contentType(MediaType.APPLICATION_JSON).content(charter(responsible)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/v1/expeditions/" + id))
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void returnsThePlanWithoutDomainTypes() throws Exception {
        UUID id = UUID.randomUUID();
        UUID responsible = UUID.randomUUID();
        ExpeditionId expeditionId = new ExpeditionId(id);
        when(consult.of(expeditionId)).thenReturn(new PlanSnapshot(
                expeditionId,
                1,
                Optional.empty(),
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
        ));

        mvc.perform(get("/v1/expeditions/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.charter.objectives[0]").value("Map wetland"))
                .andExpect(jsonPath("$.charter.zones[0]").value("Delta"))
                .andExpect(jsonPath("$.supersedes").doesNotExist())
                .andExpect(jsonPath("$.itinerary").isEmpty());
    }

    static String charter(UUID responsible) {
        return """
                {"objectives":["Map wetland"],"period":{"start":"2026-11-01T00:00:00Z","end":"2026-11-06T00:00:00Z"},"zones":["Delta"],"responsibles":["%s"],"restrictions":["Daylight only"]}
                """.formatted(responsible);
    }
}
