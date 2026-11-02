package edu.itba.fieldops.api.expedition;

import edu.itba.fieldops.api.expedition.ExpeditionRequests.FinishActivityRequest;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.ActivityExecutionResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.IncidentResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.ObservationResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.RunResponse;
import edu.itba.fieldops.api.json.Consumptions;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.tracking.ActivityExecution;
import edu.itba.fieldops.domain.tracking.ActivityReport;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.domain.tracking.Observation;
import edu.itba.fieldops.domain.tracking.Outcome;
import edu.itba.fieldops.domain.usecase.expedition.RunSnapshot;

final class RunMapping {
    private RunMapping() {
    }

    static ActivityReport activityReport(FinishActivityRequest request) {
        Outcome result = new Outcome(request.result());
        return request.consumption() == null
                ? ActivityReport.of(result)
                : new ActivityReport(result, Consumptions.toConsumption(request.consumption()));
    }

    static RunResponse run(RunSnapshot snapshot) {
        return new RunResponse(
                snapshot.expeditionId().value(),
                snapshot.inForce().value(),
                snapshot.status().name(),
                snapshot.activities().stream().map(RunMapping::execution).toList(),
                snapshot.incidents().stream().map(RunMapping::incident).toList(),
                snapshot.observations().stream().map(RunMapping::observation).toList()
        );
    }

    static IncidentResponse incident(Incident incident) {
        return new IncidentResponse(
                incident.description().text(),
                incident.at(),
                incident.activityId().map(ActivityId::value).orElse(null)
        );
    }

    static ObservationResponse observation(Observation observation) {
        return new ObservationResponse(observation.text().text(), observation.at());
    }

    private static ActivityExecutionResponse execution(ActivityExecution execution) {
        return new ActivityExecutionResponse(
                execution.activityId().value(),
                execution.startedAt(),
                execution.finishedAt().orElse(null),
                execution.result().map(Outcome::text).orElse(null),
                Consumptions.toResponse(execution.consumed())
        );
    }
}
