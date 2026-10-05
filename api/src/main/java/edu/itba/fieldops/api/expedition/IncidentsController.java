package edu.itba.fieldops.api.expedition;

import edu.itba.fieldops.api.expedition.ExpeditionRequests.IncidentRequest;
import edu.itba.fieldops.api.json.Responses;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.usecase.expedition.RecordIncident;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public final class IncidentsController {
    private final RecordIncident incidents;

    public IncidentsController(RecordIncident incidents) {
        this.incidents = incidents;
    }

    @PostMapping("/v1/expeditions/{id}/incidents")
    public ResponseEntity<Void> record(@PathVariable UUID id, @Valid @RequestBody IncidentRequest request) {
        ExpeditionId expeditionId = new ExpeditionId(id);
        if (request.activityId() == null) {
            incidents.record(expeditionId, request.description());
        } else {
            incidents.record(expeditionId, request.description(), new ActivityId(request.activityId()));
        }
        return Responses.noContent();
    }
}
