package edu.itba.fieldops.api.expedition;

import edu.itba.fieldops.api.expedition.ExpeditionRequests.AcceptWarningRequest;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.ValidationResponse;
import edu.itba.fieldops.api.json.Responses;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.usecase.expedition.ReviewExpedition;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public final class ReviewController {
    private final ReviewExpedition review;

    public ReviewController(ReviewExpedition review) {
        this.review = review;
    }

    @PostMapping("/v1/expeditions/{id}/submission")
    public ResponseEntity<Void> submit(@PathVariable UUID id) {
        review.submit(new ExpeditionId(id));
        return Responses.noContent();
    }

    @DeleteMapping("/v1/expeditions/{id}/submission")
    public ResponseEntity<Void> returnToDraft(@PathVariable UUID id) {
        review.returnToDraft(new ExpeditionId(id));
        return Responses.noContent();
    }

    @GetMapping("/v1/expeditions/{id}/validation")
    public ValidationResponse validate(@PathVariable UUID id) {
        return ExpeditionMapping.validation(review.validate(new ExpeditionId(id)));
    }

    @PostMapping("/v1/expeditions/{id}/accepted-warnings")
    public ResponseEntity<Void> acceptWarning(@PathVariable UUID id, @Valid @RequestBody AcceptWarningRequest request) {
        review.acceptWarning(new ExpeditionId(id), ExpeditionMapping.warning(request));
        return Responses.noContent();
    }
}
