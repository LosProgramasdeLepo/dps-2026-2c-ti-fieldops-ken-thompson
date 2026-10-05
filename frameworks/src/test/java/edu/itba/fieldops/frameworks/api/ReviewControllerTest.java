package edu.itba.fieldops.frameworks.api;

import edu.itba.fieldops.api.expedition.ReviewController;
import edu.itba.fieldops.domain.assessment.IssueSeverity;
import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.assessment.ValidationResult;
import edu.itba.fieldops.domain.expedition.ExpeditionNotApprovable;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.usecase.expedition.ReviewExpedition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ReviewExpedition review;

    @Test
    void submitsAndReturnsToDraft() throws Exception {
        UUID id = UUID.randomUUID();
        mvc.perform(post("/v1/expeditions/{id}/submission", id)).andExpect(status().isNoContent());
        mvc.perform(delete("/v1/expeditions/{id}/submission", id)).andExpect(status().isNoContent());
    }

    @Test
    void returnsValidationIssues() throws Exception {
        UUID id = UUID.randomUUID();
        when(review.validate(new ExpeditionId(id))).thenReturn(new ValidationResult(
                new ExpeditionId(id),
                2,
                List.of(new ValidationIssue(IssueSeverity.WARNING, "CAPACITY", "vehicle is over capacity"))
        ));

        mvc.perform(get("/v1/expeditions/{id}/validation", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.issues[0].severity").value("WARNING"))
                .andExpect(jsonPath("$.issues[0].code").value("CAPACITY"));
    }

    @Test
    void acceptsAWarning() throws Exception {
        mvc.perform(post("/v1/expeditions/{id}/accepted-warnings", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"issue":{"severity":"WARNING","code":"CAPACITY","message":"vehicle is over capacity"},"justification":"One extra passenger walks","acceptedBy":"%s"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isNoContent());
    }

    @Test
    void aCriticalSubmissionConflicts() throws Exception {
        doThrow(new ExpeditionNotApprovable("critical validation issues remain")).when(review).submit(any());

        mvc.perform(post("/v1/expeditions/{id}/submission", UUID.randomUUID()))
                .andExpect(status().isConflict());
    }
}
