package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.shared.InvalidValue;

import java.util.Objects;

public record AcceptedWarning(ValidationIssue issue, Justification justification, PersonId acceptedBy) {
    public AcceptedWarning {
        Objects.requireNonNull(issue, "accepted issue");
        if (issue.isCritical()) {
            throw new InvalidValue("critical issues cannot be accepted as warnings");
        }
        Objects.requireNonNull(justification, "justification");
        Objects.requireNonNull(acceptedBy, "accepted by");
    }
}
