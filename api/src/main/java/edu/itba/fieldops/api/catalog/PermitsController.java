package edu.itba.fieldops.api.catalog;

import edu.itba.fieldops.api.catalog.CatalogRequests.RegisterPermitRequest;
import edu.itba.fieldops.api.json.Periods;
import edu.itba.fieldops.api.json.ResourceIdResponse;
import edu.itba.fieldops.api.json.Responses;
import edu.itba.fieldops.domain.shared.PermitKind;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.usecase.catalog.AdministerPermits;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public final class PermitsController {
    private final AdministerPermits permits;

    public PermitsController(AdministerPermits permits) {
        this.permits = permits;
    }

    @PostMapping("/v1/permits")
    public ResponseEntity<ResourceIdResponse> registerPermit(@Valid @RequestBody RegisterPermitRequest request) {
        return Responses.created(
                "/v1/permits",
                permits.registerPermit(new PermitKind(request.kind()), new WorkZone(request.zone()), Periods.toPeriod(request.validity())).value()
        );
    }
}
