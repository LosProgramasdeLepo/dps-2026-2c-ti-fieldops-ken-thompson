package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.Texts;

import java.util.Objects;

public record AcceptedWarning(ValidationIssue issue, String justification, PersonId acceptedBy) {
    public AcceptedWarning {
        Objects.requireNonNull(issue, "accepted issue");
        if (issue.isCritical()) {
            throw new InvalidValue("critical issues cannot be accepted as warnings");
        }
        justification = Texts.required(justification, "justification");
        Objects.requireNonNull(acceptedBy, "accepted by");
    }
}
