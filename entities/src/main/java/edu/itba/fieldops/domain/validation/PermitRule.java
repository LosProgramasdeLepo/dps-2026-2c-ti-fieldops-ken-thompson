package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.assessment.IssueSeverity;
import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.catalog.Permit;
import edu.itba.fieldops.domain.catalog.Permits;
import edu.itba.fieldops.domain.expedition.PlanningContext;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.shared.PermitKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PermitRule implements ValidationRule {
    @Override
    public List<ValidationIssue> check(PlanningContext context) {
        List<PermitId> attached = context.plan().permits();
        List<ValidationIssue> issues = unknownPermits(attached, context.permits());
        List<Permit> known = knownPermits(attached, context.permits());
        if (known.isEmpty() && !attached.isEmpty()) {
            return issues;
        }
        for (Activity activity : context.plan().activities()) {
            issues.addAll(uncovered(activity, known));
        }
        return issues;
    }

    private static List<ValidationIssue> unknownPermits(List<PermitId> attached, Permits permits) {
        List<ValidationIssue> issues = new ArrayList<>();
        for (PermitId permitId : attached) {
            if (permits.permit(permitId).isEmpty()) {
                issues.add(issue("RESOURCE", "unknown permit " + permitId));
            }
        }
        return issues;
    }

    private static List<Permit> knownPermits(List<PermitId> attached, Permits permits) {
        return attached.stream().map(permits::permit).flatMap(Optional::stream).toList();
    }

    private static List<ValidationIssue> uncovered(Activity activity, List<Permit> known) {
        List<ValidationIssue> issues = new ArrayList<>();
        if (known.stream().noneMatch(permit -> permit.covers(activity.zone(), activity.window()))) {
            issues.add(issue(
                    "PERMIT",
                    "activity " + activity.name()
                            + " in zone " + activity.zone().name()
                            + " during " + activity.window().start()
                            + "/" + activity.window().end()
                            + " is not covered by attached permits"
            ));
        }
        for (PermitKind kind : activity.requirements().specialPermits()) {
            if (known.stream().noneMatch(permit -> permit.kind().equals(kind) && permit.covers(activity.zone(), activity.window()))) {
                issues.add(issue(
                        "PERMIT",
                        "activity " + activity.name() + " requires a " + kind.name() + " permit for zone " + activity.zone().name()
                ));
            }
        }
        return issues;
    }

    private static ValidationIssue issue(String code, String message) {
        return new ValidationIssue(IssueSeverity.CRITICAL, code, message);
    }
}
