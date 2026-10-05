package edu.itba.fieldops.api.expedition;

import edu.itba.fieldops.api.expedition.ExpeditionRequests.CharterRequest;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.ExpeditionResponse;
import edu.itba.fieldops.api.json.ResourceIdResponse;
import edu.itba.fieldops.api.json.Responses;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.usecase.expedition.ConsultExpedition;
import edu.itba.fieldops.usecase.expedition.DraftExpedition;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public final class ExpeditionsController {
    private final DraftExpedition drafts;
    private final ConsultExpedition consult;

    public ExpeditionsController(DraftExpedition drafts, ConsultExpedition consult) {
        this.drafts = drafts;
        this.consult = consult;
    }

    @PostMapping("/v1/expeditions")
    public ResponseEntity<ResourceIdResponse> draft(@Valid @RequestBody CharterRequest request) {
        return Responses.created("/v1/expeditions", drafts.draft(ExpeditionMapping.charter(request)).value());
    }

    @GetMapping("/v1/expeditions/{id}")
    public ExpeditionResponse consult(@PathVariable UUID id) {
        return ExpeditionMapping.expedition(consult.of(new ExpeditionId(id)));
    }
}
