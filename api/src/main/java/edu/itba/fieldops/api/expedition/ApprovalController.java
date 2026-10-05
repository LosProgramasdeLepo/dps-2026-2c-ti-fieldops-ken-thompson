package edu.itba.fieldops.api.expedition;

import edu.itba.fieldops.api.json.Responses;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.usecase.expedition.ApproveExpedition;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public final class ApprovalController {
    private final ApproveExpedition approval;

    public ApprovalController(ApproveExpedition approval) {
        this.approval = approval;
    }

    @PostMapping("/v1/expeditions/{id}/approval")
    public ResponseEntity<Void> approve(@PathVariable UUID id) {
        approval.approve(new ExpeditionId(id));
        return Responses.noContent();
    }
}
