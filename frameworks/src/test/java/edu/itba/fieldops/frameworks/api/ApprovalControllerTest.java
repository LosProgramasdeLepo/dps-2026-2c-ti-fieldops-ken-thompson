package edu.itba.fieldops.frameworks.api;

import edu.itba.fieldops.api.expedition.ApprovalController;
import edu.itba.fieldops.domain.expedition.ExpeditionNotApprovable;
import edu.itba.fieldops.usecase.expedition.ApproveExpedition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApprovalController.class)
class ApprovalControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ApproveExpedition approval;

    @Test
    void approvesWithNoBody() throws Exception {
        mvc.perform(post("/v1/expeditions/{id}/approval", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    void aPlanThatCannotBeApprovedConflicts() throws Exception {
        doThrow(new ExpeditionNotApprovable("warnings must be accepted")).when(approval).approve(any());

        mvc.perform(post("/v1/expeditions/{id}/approval", UUID.randomUUID()))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
