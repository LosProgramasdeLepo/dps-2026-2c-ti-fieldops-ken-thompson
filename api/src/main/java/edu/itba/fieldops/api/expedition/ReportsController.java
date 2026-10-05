package edu.itba.fieldops.api.expedition;

import edu.itba.fieldops.api.expedition.ExpeditionResponses.EstimateResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.ReportResponse;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.usecase.report.EstimateExpedition;
import edu.itba.fieldops.usecase.report.ReportExpedition;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public final class ReportsController {
    private final EstimateExpedition estimates;
    private final ReportExpedition reports;

    public ReportsController(EstimateExpedition estimates, ReportExpedition reports) {
        this.estimates = estimates;
        this.reports = reports;
    }

    @GetMapping("/v1/expeditions/{id}/estimate")
    public EstimateResponse estimate(@PathVariable UUID id) {
        return ExpeditionMapping.estimate(estimates.of(new ExpeditionId(id)));
    }

    @GetMapping("/v1/expeditions/{id}/report")
    public ReportResponse report(@PathVariable UUID id) {
        return ExpeditionMapping.report(reports.of(new ExpeditionId(id)));
    }
}
