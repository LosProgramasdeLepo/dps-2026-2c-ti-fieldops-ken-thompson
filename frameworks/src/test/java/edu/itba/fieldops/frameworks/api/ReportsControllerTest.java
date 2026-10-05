package edu.itba.fieldops.frameworks.api;

import edu.itba.fieldops.api.expedition.ReportsController;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.report.Estimate;
import edu.itba.fieldops.domain.report.OperationalReport;
import edu.itba.fieldops.domain.report.OperationalStatus;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.usecase.report.EstimateExpedition;
import edu.itba.fieldops.usecase.report.ReportExpedition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportsController.class)
class ReportsControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EstimateExpedition estimates;

    @MockitoBean
    private ReportExpedition reports;

    @Test
    void returnsTheEstimate() throws Exception {
        UUID id = UUID.randomUUID();
        org.mockito.Mockito.when(estimates.of(new ExpeditionId(id)))
                .thenReturn(new Estimate(Duration.ofHours(4), RiskLevel.MEDIUM, Map.of()));

        mvc.perform(get("/v1/expeditions/{id}/estimate", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duration").value("PT4H"))
                .andExpect(jsonPath("$.risk").value("MEDIUM"));
    }

    @Test
    void returnsTheReport() throws Exception {
        UUID id = UUID.randomUUID();
        org.mockito.Mockito.when(reports.of(new ExpeditionId(id))).thenReturn(new OperationalReport(
                OperationalStatus.APPROVED,
                1,
                0,
                0,
                Duration.ofHours(4),
                RiskLevel.LOW,
                Map.of(),
                Map.of(),
                List.of(),
                List.of(),
                List.of()
        ));

        mvc.perform(get("/v1/expeditions/{id}/report", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.plannedActivities").value(1));
    }
}
