package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.shared.InvalidValue;

import java.util.ArrayList;
import java.util.List;

final class AcceptedWarnings {
    private final List<AcceptedWarning> warnings = new ArrayList<>();

    void accept(AcceptedWarning warning) {
        if (covers(warning.issue())) {
            throw new InvalidValue("warning already accepted: " + warning.issue().code());
        }
        warnings.add(warning);
    }

    boolean covers(ValidationIssue issue) {
        return warnings.stream().anyMatch(entry -> entry.issue().equals(issue));
    }

    List<AcceptedWarning> all() {
        return List.copyOf(warnings);
    }

    void clear() {
        warnings.clear();
    }
}
