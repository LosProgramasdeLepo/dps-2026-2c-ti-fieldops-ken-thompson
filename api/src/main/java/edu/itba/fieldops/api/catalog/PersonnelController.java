package edu.itba.fieldops.api.catalog;

import edu.itba.fieldops.api.catalog.CatalogRequests.AvailabilityChangeRequest;
import edu.itba.fieldops.api.catalog.CatalogRequests.NameRequest;
import edu.itba.fieldops.api.catalog.CatalogRequests.RegisterPersonRequest;
import edu.itba.fieldops.api.json.Periods;
import edu.itba.fieldops.api.json.ResourceIdResponse;
import edu.itba.fieldops.api.json.Responses;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.usecase.catalog.AdministerPersonnel;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public final class PersonnelController {
    private final AdministerPersonnel personnel;

    public PersonnelController(AdministerPersonnel personnel) {
        this.personnel = personnel;
    }

    @PostMapping("/v1/certifications")
    public ResponseEntity<ResourceIdResponse> registerCertification(@Valid @RequestBody NameRequest request) {
        return Responses.created("/v1/certifications", personnel.registerCertification(request.name()).value());
    }

    @PostMapping("/v1/people")
    public ResponseEntity<ResourceIdResponse> registerPerson(@Valid @RequestBody RegisterPersonRequest request) {
        PersonId id = personnel.registerPerson(
                request.name(),
                request.certifications().stream().map(CertificationId::new).toList(),
                Periods.toAvailability(request.availability())
        );
        return Responses.created("/v1/people", id.value());
    }

    @PutMapping("/v1/people/{id}/certifications/{certificationId}")
    public ResponseEntity<Void> certify(@PathVariable UUID id, @PathVariable UUID certificationId) {
        personnel.certify(new PersonId(id), new CertificationId(certificationId));
        return Responses.noContent();
    }

    @PutMapping("/v1/people/{id}/availability")
    public ResponseEntity<Void> changeAvailability(@PathVariable UUID id, @Valid @RequestBody AvailabilityChangeRequest request) {
        personnel.changeAvailability(new PersonId(id), Periods.toAvailability(request.availability()));
        return Responses.noContent();
    }
}
