package edu.itba.fieldops.api.expedition;

import edu.itba.fieldops.api.expedition.ExpeditionRequests.DecisionRequest;
import edu.itba.fieldops.api.expedition.ExpeditionRequests.DelayRequest;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.ProposalResponse;
import edu.itba.fieldops.api.json.ResourceIdResponse;
import edu.itba.fieldops.api.json.Responses;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.ProposalId;
import edu.itba.fieldops.usecase.expedition.ReplanExpedition;
import edu.itba.fieldops.usecase.expedition.ReviewReplanProposal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public final class ReplanController {
    private final ReplanExpedition replan;
    private final ReviewReplanProposal proposals;

    public ReplanController(ReplanExpedition replan, ReviewReplanProposal proposals) {
        this.replan = replan;
        this.proposals = proposals;
    }

    @PostMapping("/v1/expeditions/{id}/revisions")
    public ResponseEntity<ResourceIdResponse> revise(@PathVariable UUID id) {
        return Responses.created("/v1/expeditions", replan.revise(new ExpeditionId(id)).value());
    }

    @DeleteMapping("/v1/expeditions/{id}/activities/{activityId}")
    public ResponseEntity<Void> cancel(@PathVariable UUID id, @PathVariable UUID activityId) {
        replan.cancel(new ExpeditionId(id), new ActivityId(activityId));
        return Responses.noContent();
    }

    @PostMapping("/v1/expeditions/{id}/activities/{activityId}/delay")
    public ResponseEntity<Void> delay(@PathVariable UUID id, @PathVariable UUID activityId, @Valid @RequestBody DelayRequest request) {
        replan.delay(new ExpeditionId(id), new ActivityId(activityId), request.delay());
        return Responses.noContent();
    }

    @PostMapping("/v1/expeditions/{id}/reassignments")
    public ResponseEntity<Void> replaceUnavailable(@PathVariable UUID id) {
        replan.replaceUnavailable(new ExpeditionId(id));
        return Responses.noContent();
    }

    @GetMapping("/v1/expeditions/{id}/replan-proposals")
    public List<ProposalResponse> proposals(@PathVariable UUID id) {
        return proposals.of(new ExpeditionId(id)).stream().map(ExpeditionMapping::proposal).toList();
    }

    @PostMapping("/v1/replan-proposals/{proposalId}/acceptance")
    public ResponseEntity<Void> accept(@PathVariable UUID proposalId, @Valid @RequestBody DecisionRequest request) {
        proposals.accept(new ProposalId(proposalId), new PersonId(request.responsible()));
        return Responses.noContent();
    }

    @PostMapping("/v1/replan-proposals/{proposalId}/rejection")
    public ResponseEntity<Void> reject(@PathVariable UUID proposalId, @Valid @RequestBody DecisionRequest request) {
        proposals.reject(new ProposalId(proposalId), new PersonId(request.responsible()));
        return Responses.noContent();
    }
}
