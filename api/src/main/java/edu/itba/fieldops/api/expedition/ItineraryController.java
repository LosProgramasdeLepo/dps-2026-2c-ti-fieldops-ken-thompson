package edu.itba.fieldops.api.expedition;

import edu.itba.fieldops.api.expedition.ExpeditionRequests.ActivityRequest;
import edu.itba.fieldops.api.expedition.ExpeditionRequests.BlockNode;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.BlockResponse;
import edu.itba.fieldops.api.json.ResourceIdResponse;
import edu.itba.fieldops.api.json.Responses;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.itinerary.ActivityBlock;
import edu.itba.fieldops.usecase.expedition.PlanItinerary;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
public final class ItineraryController {
    private final PlanItinerary itinerary;

    public ItineraryController(PlanItinerary itinerary) {
        this.itinerary = itinerary;
    }

    @PostMapping("/v1/expeditions/{id}/activities")
    public ResponseEntity<ResourceIdResponse> addActivity(@PathVariable UUID id, @Valid @RequestBody ActivityRequest request) {
        ExpeditionId expeditionId = new ExpeditionId(id);
        ActivityId activityId = itinerary.nextActivityId();
        itinerary.addActivity(expeditionId, ExpeditionMapping.activity(request, activityId));
        return Responses.created("/v1/expeditions/" + id + "/activities", activityId.value());
    }

    @PostMapping("/v1/expeditions/{id}/blocks")
    public ResponseEntity<BlockResponse> addBlock(@PathVariable UUID id, @Valid @RequestBody BlockNode request) {
        ActivityBlock block = ExpeditionMapping.block(request, itinerary::nextActivityId);
        itinerary.addBlock(new ExpeditionId(id), block);
        return ResponseEntity.created(URI.create("/v1/expeditions/" + id)).body(ExpeditionMapping.block(block));
    }

    @PutMapping("/v1/expeditions/{id}/activities/{activityId}/predecessors/{predecessorId}")
    public ResponseEntity<Void> addDependency(
            @PathVariable UUID id,
            @PathVariable UUID activityId,
            @PathVariable UUID predecessorId
    ) {
        itinerary.addDependency(new ExpeditionId(id), new ActivityId(activityId), new ActivityId(predecessorId));
        return Responses.noContent();
    }
}
