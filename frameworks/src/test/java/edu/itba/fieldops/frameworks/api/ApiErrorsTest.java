package edu.itba.fieldops.frameworks.api;

import edu.itba.fieldops.api.expedition.ExpeditionsController;
import edu.itba.fieldops.domain.expedition.ExpeditionNotApprovable;
import edu.itba.fieldops.domain.expedition.ExpeditionStatus;
import edu.itba.fieldops.domain.expedition.InvalidAssignment;
import edu.itba.fieldops.domain.expedition.InvalidExpeditionTransition;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.itinerary.InvalidItinerary;
import edu.itba.fieldops.domain.shared.DomainException;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.domain.tracking.InvalidActivityExecution;
import edu.itba.fieldops.usecase.expedition.ConsultExpedition;
import edu.itba.fieldops.usecase.expedition.DraftExpedition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExpeditionsController.class)
class ApiErrorsTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConsultExpedition consult;

    @MockitoBean
    private DraftExpedition drafts;

    @Test
    void unknownExpeditionInThePathIsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(consult.of(new ExpeditionId(id))).thenThrow(new UnknownResource("expedition", id.toString()));

        mvc.perform(get("/v1/expeditions/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("unknown expedition: " + id));
    }

    @Test
    void unknownReferenceInTheBodyIsUnprocessable() throws Exception {
        UUID pathId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        when(consult.of(new ExpeditionId(pathId))).thenThrow(new UnknownResource("responsible", otherId.toString()));

        mvc.perform(get("/v1/expeditions/{id}", pathId))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("unknown responsible: " + otherId));
    }

    @Test
    void invalidValueIsUnprocessable() throws Exception {
        expect(new InvalidValue("objectives must not be empty"), 422);
    }

    @Test
    void invalidItineraryIsUnprocessable() throws Exception {
        expect(new InvalidItinerary("activity window is shorter than estimated duration"), 422);
    }

    @Test
    void invalidAssignmentIsUnprocessable() throws Exception {
        expect(new InvalidAssignment("assigned quantity must be positive"), 422);
    }

    @Test
    void illegalTransitionConflicts() throws Exception {
        expect(new InvalidExpeditionTransition(ExpeditionStatus.DRAFT, "approve"), 409);
    }

    @Test
    void anExpeditionThatCannotBeApprovedConflicts() throws Exception {
        expect(new ExpeditionNotApprovable("critical validation issues remain"), 409);
    }

    @Test
    void illegalTrackingConflicts() throws Exception {
        expect(new InvalidActivityExecution("activity is outside its window"), 409);
    }

    @Test
    void anUnclassifiedDomainFailureIsUnprocessable() throws Exception {
        expect(new DomainException("future rule") {
        }, 422);
    }

    @Test
    void anUnexpectedFailureHidesItsDetail() throws Exception {
        when(consult.of(any())).thenThrow(new IllegalStateException("secret-database-url"));

        mvc.perform(get("/v1/expeditions/{id}", UUID.randomUUID()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().string(not(containsString("secret-database-url"))));
    }

    @Test
    void aMalformedIdentifierIsRejected() throws Exception {
        mvc.perform(get("/v1/expeditions/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void aCharterThatFailsValidationIsRejected() throws Exception {
        mvc.perform(post("/v1/expeditions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"objectives":[],"period":{"start":"2026-11-01T00:00:00Z","end":"2026-11-06T00:00:00Z"},"zones":["Delta"],"responsibles":["%s"],"restrictions":[]}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    private void expect(DomainException error, int status) throws Exception {
        when(consult.of(any())).thenThrow(error);
        mvc.perform(get("/v1/expeditions/{id}", UUID.randomUUID()))
                .andExpect(status().is(status))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(error.getMessage()));
    }
}
