package edu.itba.fieldops.adapters.jpa.expedition;

import edu.itba.fieldops.domain.assessment.IssueSeverity;
import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.expedition.AcceptedWarning;
import edu.itba.fieldops.domain.identity.PersonId;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public record StoredWarning(
        @Column(name = "severity", nullable = false) String severity,
        @Column(name = "code", nullable = false) String code,
        @Column(name = "message", nullable = false) String message,
        @Column(name = "justification", nullable = false) String justification,
        @Column(name = "accepted_by", nullable = false) UUID acceptedBy
) {
    static StoredWarning of(AcceptedWarning warning) {
        ValidationIssue issue = warning.issue();
        return new StoredWarning(
                issue.severity().name(),
                issue.code(),
                issue.message(),
                warning.justification(),
                warning.acceptedBy().value()
        );
    }

    AcceptedWarning toDomain() {
        return new AcceptedWarning(
                new ValidationIssue(IssueSeverity.valueOf(severity), code, message),
                justification,
                new PersonId(acceptedBy)
        );
    }
}
