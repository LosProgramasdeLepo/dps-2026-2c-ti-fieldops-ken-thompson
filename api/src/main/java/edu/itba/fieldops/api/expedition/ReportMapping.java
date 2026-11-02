package edu.itba.fieldops.api.expedition;

import edu.itba.fieldops.api.expedition.ExpeditionResponses.ActivityResultResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.ActivityRiskResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.EstimateResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.ReportResponse;
import edu.itba.fieldops.api.json.Consumptions;
import edu.itba.fieldops.domain.report.ActivityResult;
import edu.itba.fieldops.domain.report.ActivityRisk;
import edu.itba.fieldops.domain.report.Estimate;
import edu.itba.fieldops.domain.report.OperationalReport;

final class ReportMapping {
    private ReportMapping() {
    }

    static EstimateResponse estimate(Estimate estimate) {
        return new EstimateResponse(
                estimate.duration().toString(),
                estimate.risk().name(),
                Consumptions.toResponse(estimate.estimatedConsumption())
        );
    }

    static ReportResponse report(OperationalReport report) {
        return new ReportResponse(
                report.status().name(),
                report.plannedActivities(),
                report.startedActivities(),
                report.finishedActivities(),
                report.duration().toString(),
                report.risk().name(),
                report.risks().stream().map(ReportMapping::risk).toList(),
                Consumptions.toResponse(report.consumption()),
                Consumptions.toResponse(report.estimatedConsumption()),
                report.incidents().stream().map(RunMapping::incident).toList(),
                report.observations().stream().map(RunMapping::observation).toList(),
                report.activityResults().stream().map(ReportMapping::result).toList()
        );
    }

    private static ActivityRiskResponse risk(ActivityRisk risk) {
        return new ActivityRiskResponse(risk.activityId().value(), risk.risk().name());
    }

    private static ActivityResultResponse result(ActivityResult result) {
        return new ActivityResultResponse(result.activityId().value(), result.result().text());
    }
}
