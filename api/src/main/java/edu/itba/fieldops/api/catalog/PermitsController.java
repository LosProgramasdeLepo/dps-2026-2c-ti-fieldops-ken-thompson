package edu.itba.fieldops.api.catalog;

import edu.itba.fieldops.api.catalog.CatalogRequests.RegisterPermitRequest;
import edu.itba.fieldops.api.catalog.CatalogResponses.PermitResponse;
import edu.itba.fieldops.api.json.PageQuery;
import edu.itba.fieldops.api.json.PageResponse;
import edu.itba.fieldops.api.json.Pages;
import edu.itba.fieldops.api.json.Periods;
import edu.itba.fieldops.api.json.ResourceIdResponse;
import edu.itba.fieldops.api.json.Responses;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.shared.PermitKind;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.usecase.catalog.AdministerPermits;
import edu.itba.fieldops.usecase.catalog.ConsultPermits;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public final class PermitsController {
    private final AdministerPermits permits;
    private final ConsultPermits consult;

    public PermitsController(AdministerPermits permits, ConsultPermits consult) {
        this.permits = permits;
        this.consult = consult;
    }

    @PostMapping("/v1/permits")
    public ResponseEntity<ResourceIdResponse> registerPermit(@Valid @RequestBody RegisterPermitRequest request) {
        return Responses.created(
                "/v1/permits",
                permits.registerPermit(new PermitKind(request.kind()), new WorkZone(request.zone()), Periods.toPeriod(request.validity())).value()
        );
    }

    @GetMapping("/v1/permits")
    public ResponseEntity<PageResponse<PermitResponse>> permits(@Valid PageQuery query) {
        return Pages.toResponse(consult.permits(Pages.toRequest(query)), CatalogMapping::permit);
    }

    @GetMapping("/v1/permits/{id}")
    public PermitResponse permit(@PathVariable UUID id) {
        return CatalogMapping.permit(consult.permit(new PermitId(id)));
    }
}
