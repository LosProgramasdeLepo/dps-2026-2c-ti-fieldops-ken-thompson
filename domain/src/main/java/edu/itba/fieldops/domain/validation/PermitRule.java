package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.assessment.IssueSeverity;
import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.catalog.Permits;
import edu.itba.fieldops.domain.catalog.Permit;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.itinerary.Activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PermitRule implements ValidationRule {
    @Override
    public List<ValidationIssue> check(ValidationContext context) {
        Expedition expedition = context.expedition();
        Permits permits = context.permits();
        List<PermitId> attached = expedition.permits();
        List<Permit> known = new ArrayList<>();
        List<ValidationIssue> issues = new ArrayList<>();
        for (PermitId permitId : attached) {
            Optional<Permit> permit = permits.permit(permitId);
            if (permit.isEmpty()) {
                issues.add(issue("RESOURCE", "unknown permit " + permitId));
            } else {
                known.add(permit.get());
            }
        }
        if (known.isEmpty() && !attached.isEmpty()) {
            return issues;
        }
        for (Activity activity : expedition.itinerary()) {
            boolean covered = false;
            for (Permit permit : known) {
                if (permit.covers(activity.zone(), activity.window())) {
                    covered = true;
                    break;
                }
            }
            if (!covered) {
                issues.add(issue(
                        "PERMIT",
                        "activity " + activity.name()
                                + " in zone " + activity.zone().name()
                                + " during " + activity.window().start()
                                + "/" + activity.window().end()
                                + " is not covered by attached permits"
                ));
            }
        }
        return issues;
    }

    private static ValidationIssue issue(String code, String message) {
        return new ValidationIssue(IssueSeverity.CRITICAL, code, message);
    }
}
