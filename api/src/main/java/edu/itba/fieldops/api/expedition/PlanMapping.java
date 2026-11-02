package edu.itba.fieldops.api.expedition;

import edu.itba.fieldops.api.expedition.ExpeditionRequests.CharterRequest;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.AcceptedWarningResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.CharterResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.ExpeditionResponse;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.ExpeditionSummaryResponse;
import edu.itba.fieldops.api.json.Periods;
import edu.itba.fieldops.domain.expedition.AcceptedWarning;
import edu.itba.fieldops.domain.expedition.ExpeditionCharter;
import edu.itba.fieldops.domain.expedition.Objective;
import edu.itba.fieldops.domain.expedition.Restriction;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.usecase.expedition.PlanSnapshot;

final class PlanMapping {
    private PlanMapping() {
    }

    static ExpeditionCharter charter(CharterRequest request) {
        return new ExpeditionCharter(
                request.objectives().stream().map(Objective::new).toList(),
                Periods.toPeriod(request.period()),
                request.zones().stream().map(WorkZone::new).toList(),
                request.responsibles().stream().map(PersonId::new).toList(),
                request.restrictions().stream().map(Restriction::new).toList()
        );
    }

    static ExpeditionResponse expedition(PlanSnapshot snapshot) {
        return new ExpeditionResponse(
                snapshot.id().value(),
                snapshot.version(),
                snapshot.supersedes().map(ExpeditionId::value).orElse(null),
                snapshot.status().name(),
                charter(snapshot.charter()),
                snapshot.itinerary().stream().map(ItineraryMapping::item).toList(),
                AssignmentMapping.assignments(snapshot.assignments()),
                snapshot.permits().stream().map(PermitId::value).toList(),
                snapshot.acceptedWarnings().stream().map(PlanMapping::warning).toList()
        );
    }

    static ExpeditionSummaryResponse summary(PlanSnapshot snapshot) {
        return new ExpeditionSummaryResponse(
                snapshot.id().value(),
                snapshot.version(),
                snapshot.supersedes().map(ExpeditionId::value).orElse(null),
                snapshot.status().name(),
                charter(snapshot.charter())
        );
    }

    private static CharterResponse charter(ExpeditionCharter charter) {
        return new CharterResponse(
                charter.objectives().stream().map(Objective::text).toList(),
                Periods.toResponse(charter.period()),
                charter.zones().stream().map(WorkZone::name).toList(),
                charter.responsibles().stream().map(PersonId::value).toList(),
                charter.restrictions().stream().map(Restriction::text).toList()
        );
    }

    private static AcceptedWarningResponse warning(AcceptedWarning warning) {
        return new AcceptedWarningResponse(
                warning.issue().severity().name(),
                warning.issue().code(),
                warning.issue().message(),
                warning.justification().text(),
                warning.acceptedBy().value()
        );
    }
}
