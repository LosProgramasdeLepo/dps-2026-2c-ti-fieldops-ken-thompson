package edu.itba.fieldops.api.expedition;

import edu.itba.fieldops.api.expedition.ExpeditionRequests.FinishActivityRequest;
import edu.itba.fieldops.api.expedition.ExpeditionRequests.ObservationRequest;
import edu.itba.fieldops.api.expedition.ExpeditionRequests.StartActivityRequest;
import edu.itba.fieldops.api.json.Responses;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.usecase.expedition.TrackExpedition;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public final class RunController {
    private final TrackExpedition tracking;

    public RunController(TrackExpedition tracking) {
        this.tracking = tracking;
    }

    @PostMapping("/v1/expeditions/{id}/run")
    public ResponseEntity<Void> start(@PathVariable UUID id) {
        tracking.start(new ExpeditionId(id));
        return Responses.noContent();
    }

    @PostMapping("/v1/expeditions/{id}/run/suspension")
    public ResponseEntity<Void> suspend(@PathVariable UUID id) {
        tracking.suspend(new ExpeditionId(id));
        return Responses.noContent();
    }

    @DeleteMapping("/v1/expeditions/{id}/run/suspension")
    public ResponseEntity<Void> resume(@PathVariable UUID id) {
        tracking.resume(new ExpeditionId(id));
        return Responses.noContent();
    }

    @PostMapping("/v1/expeditions/{id}/run/completion")
    public ResponseEntity<Void> finish(@PathVariable UUID id) {
        tracking.finish(new ExpeditionId(id));
        return Responses.noContent();
    }

    @PostMapping("/v1/expeditions/{id}/run/activities")
    public ResponseEntity<Void> startActivity(@PathVariable UUID id, @Valid @RequestBody StartActivityRequest request) {
        tracking.startActivity(new ExpeditionId(id), new ActivityId(request.activityId()));
        return Responses.noContent();
    }

    @PostMapping("/v1/expeditions/{id}/run/activities/{activityId}/completion")
    public ResponseEntity<Void> finishActivity(
            @PathVariable UUID id,
            @PathVariable UUID activityId,
            @Valid @RequestBody FinishActivityRequest request
    ) {
        tracking.finishActivity(new ExpeditionId(id), new ActivityId(activityId), request.result());
        return Responses.noContent();
    }

    @PostMapping("/v1/expeditions/{id}/run/observations")
    public ResponseEntity<Void> addObservation(@PathVariable UUID id, @Valid @RequestBody ObservationRequest request) {
        tracking.addObservation(new ExpeditionId(id), request.text());
        return Responses.noContent();
    }
}
