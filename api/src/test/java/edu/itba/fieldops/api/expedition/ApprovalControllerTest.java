package edu.itba.fieldops.api.expedition;

import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.usecase.expedition.ApproveExpedition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApprovalController.class)
class ApprovalControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ApproveExpedition approval;

    @Test
    void approvesWithNoBody() throws Exception {
        UUID id = UUID.randomUUID();
        mvc.perform(post("/v1/expeditions/{id}/approval", id))
                .andExpect(status().isNoContent());
        verify(approval).approve(new ExpeditionId(id));
    }
}
