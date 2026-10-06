package edu.itba.fieldops.frameworks.api;

import edu.itba.fieldops.api.expedition.ExpeditionsController;
import edu.itba.fieldops.domain.expedition.ExpeditionCharter;
import edu.itba.fieldops.domain.expedition.ExpeditionStatus;
import edu.itba.fieldops.domain.expedition.Objective;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.usecase.expedition.ConsultExpedition;
import edu.itba.fieldops.usecase.expedition.DraftExpedition;
import edu.itba.fieldops.usecase.expedition.PlanSnapshot;
import edu.itba.fieldops.usecase.shared.Page;
import edu.itba.fieldops.usecase.shared.PageRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
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
        when(consult.of(new ExpeditionId(id))).thenReturn(plan(id));

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

    @Test
    void listsPlansAsSummaries() throws Exception {
        UUID id = UUID.randomUUID();
        PageRequest request = new PageRequest(0, 20);
        when(consult.all(request)).thenReturn(new Page<>(List.of(plan(id)), request, 1));

        mvc.perform(get("/v1/expeditions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(id.toString()))
                .andExpect(jsonPath("$.items[0].status").value("DRAFT"))
                .andExpect(jsonPath("$.items[0].charter.objectives[0]").value("Map wetland"))
                .andExpect(jsonPath("$.items[0].itinerary").doesNotExist())
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void readsAnActivityOfThePlan() throws Exception {
        UUID id = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        when(consult.activity(new ExpeditionId(id), new ActivityId(activityId))).thenReturn(Activity.transit()
                .named(new ActivityId(activityId), "Crossing")
                .estimated(Duration.ofHours(2), RiskLevel.LOW)
                .in(new WorkZone("Delta"), new TimePeriod(Instant.parse("2026-11-01T08:00:00Z"), Instant.parse("2026-11-01T10:00:00Z")))
                .build());

        mvc.perform(get("/v1/expeditions/{id}/activities/{activityId}", id, activityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(activityId.toString()))
                .andExpect(jsonPath("$.name").value("Crossing"))
                .andExpect(jsonPath("$.estimatedDuration").value("PT2H"));
    }

    @Test
    void anActivityOutsideThePlanIsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        when(consult.activity(new ExpeditionId(id), new ActivityId(activityId))).thenThrow(new UnknownResource("activity", activityId.toString()));

        mvc.perform(get("/v1/expeditions/{id}/activities/{activityId}", id, activityId))
                .andExpect(status().isNotFound());
    }

    static PlanSnapshot plan(UUID id) {
        return new PlanSnapshot(
                new ExpeditionId(id),
                1,
                Optional.empty(),
                ExpeditionStatus.DRAFT,
                new ExpeditionCharter(
                        List.of(new Objective("Map wetland")),
                        new TimePeriod(Instant.parse("2026-11-01T00:00:00Z"), Instant.parse("2026-11-06T00:00:00Z")),
                        List.of(new WorkZone("Delta")),
                        List.of(new PersonId(UUID.randomUUID())),
                        List.of()
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    static String charter(UUID responsible) {
        return """
                {"objectives":["Map wetland"],"period":{"start":"2026-11-01T00:00:00Z","end":"2026-11-06T00:00:00Z"},"zones":["Delta"],"responsibles":["%s"],"restrictions":["Daylight only"]}
                """.formatted(responsible);
    }
}
