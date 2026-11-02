package edu.itba.fieldops.api.expedition;

import edu.itba.fieldops.api.config.FieldOpsProperties;
import edu.itba.fieldops.api.error.FeatureDisabled;
import edu.itba.fieldops.api.expedition.ExpeditionRequests.AssignmentRequest;
import edu.itba.fieldops.api.expedition.ExpeditionResponses.AssignmentResponse;
import edu.itba.fieldops.api.json.Responses;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.usecase.expedition.AssignResources;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public final class AssignmentsController {
    private final AssignResources assignments;
    private final FieldOpsProperties properties;

    public AssignmentsController(AssignResources assignments, FieldOpsProperties properties) {
        this.assignments = assignments;
        this.properties = properties;
    }

    @PostMapping("/v1/expeditions/{id}/assignments")
    public ResponseEntity<Void> addAssignment(@PathVariable UUID id, @Valid @RequestBody AssignmentRequest request) {
        assignments.addAssignment(new ExpeditionId(id), AssignmentMapping.assignment(request));
        return Responses.noContent();
    }

    @PutMapping("/v1/expeditions/{id}/permits/{permitId}")
    public ResponseEntity<Void> addPermit(@PathVariable UUID id, @PathVariable UUID permitId) {
        assignments.addPermit(new ExpeditionId(id), new PermitId(permitId));
        return Responses.noContent();
    }

    @GetMapping("/v1/expeditions/{id}/assignment-suggestions")
    public List<AssignmentResponse> suggest(@PathVariable UUID id) {
        if (!properties.getFeatures().isAssignmentSuggestions()) {
            throw new FeatureDisabled("Assignment suggestions");
        }
        return AssignmentMapping.assignments(assignments.suggest(new ExpeditionId(id)));
    }
}
