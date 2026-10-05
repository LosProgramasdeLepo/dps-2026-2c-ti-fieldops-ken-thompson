package edu.itba.fieldops.domain.assessment;

import edu.itba.fieldops.domain.identity.ExpeditionId;

import java.util.List;
import java.util.Objects;

public record ValidationResult(ExpeditionId expeditionId, int version, List<ValidationIssue> issues) {
    public ValidationResult {
        Objects.requireNonNull(expeditionId, "expedition id");
        issues = List.copyOf(issues);
    }

    public boolean hasCritical() {
        return issues.stream().anyMatch(ValidationIssue::isCritical);
    }

    public List<ValidationIssue> warnings() {
        return issues.stream().filter(ValidationIssue::isWarning).toList();
    }
}
