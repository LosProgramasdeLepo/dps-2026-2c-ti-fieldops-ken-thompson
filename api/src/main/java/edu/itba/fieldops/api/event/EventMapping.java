package edu.itba.fieldops.api.event;

import edu.itba.fieldops.api.event.EventResponses.EventResponse;
import edu.itba.fieldops.api.json.Consumptions;
import edu.itba.fieldops.domain.event.ActivityCompleted;
import edu.itba.fieldops.domain.event.ActivityStarted;
import edu.itba.fieldops.domain.event.DomainEvent;
import edu.itba.fieldops.domain.event.ExpeditionApproved;
import edu.itba.fieldops.domain.event.IncidentReported;
import edu.itba.fieldops.domain.event.RecordedEvent;
import edu.itba.fieldops.domain.event.ReplanDecided;
import edu.itba.fieldops.domain.event.ReplanProposed;
import edu.itba.fieldops.domain.event.RunFinished;
import edu.itba.fieldops.domain.event.RunStarted;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;

import java.util.LinkedHashMap;
import java.util.Map;

final class EventMapping {
    private EventMapping() {
    }

    static EventResponse event(RecordedEvent recorded) {
        DomainEvent event = recorded.event();
        return new EventResponse(
                recorded.id().value(),
                recorded.sequence(),
                event.type(),
                event.expeditionId().value(),
                event.occurredAt(),
                recorded.recordedAt(),
                payload(event)
        );
    }

    private static Map<String, Object> payload(DomainEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        switch (event) {
            case ExpeditionApproved approved -> {
                payload.put("version", approved.version());
                approved.superseded().map(ExpeditionId::value).ifPresent(id -> payload.put("supersededId", id));
            }
            case RunStarted started -> {
            }
            case ActivityStarted started -> payload.put("activityId", started.activityId().value());
            case ActivityCompleted completed -> {
                payload.put("activityId", completed.activityId().value());
                payload.put("startedAt", completed.startedAt());
                payload.put("result", completed.result().text());
                payload.put("consumed", Consumptions.toResponse(completed.consumed()));
            }
            case IncidentReported reported -> {
                payload.put("incidentId", reported.incidentId().value());
                reported.activityId().map(ActivityId::value).ifPresent(id -> payload.put("activityId", id));
                payload.put("description", reported.description().text());
            }
            case RunFinished finished -> {
            }
            case ReplanProposed proposed -> {
                payload.put("proposalId", proposed.proposalId().value());
                payload.put("suggestedId", proposed.suggested().value());
                payload.put("incidentId", proposed.incidentId().value());
            }
            case ReplanDecided decided -> {
                payload.put("proposalId", decided.proposalId().value());
                payload.put("decision", decided.decision().name());
                payload.put("decidedBy", decided.decidedBy().value());
            }
        }
        return payload;
    }
}
