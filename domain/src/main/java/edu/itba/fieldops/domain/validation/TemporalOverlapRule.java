package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.assessment.IssueSeverity;
import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.catalog.BookableResources;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.TemporalBooking;

import java.util.ArrayList;
import java.util.List;

public final class TemporalOverlapRule implements ValidationRule {
    @Override
    public List<ValidationIssue> check(ValidationContext context) {
        BookableResources resources = context.bookable();
        List<TemporalBooking> own = TemporalBooking.of(context.expedition());
        List<ValidationIssue> issues = new ArrayList<>();
        for (TemporalBooking booking : own) {
            if (booking.unavailableIn(resources)) {
                issues.add(critical(
                        "AVAILABILITY",
                        booking.label() + " is not available during "
                                + booking.window().start() + "/" + booking.window().end()
                ));
            }
        }
        issues.addAll(conflicts(own, own, true));
        for (Expedition peer : context.occupying().plans()) {
            issues.addAll(conflicts(own, TemporalBooking.of(peer), false));
        }
        return issues;
    }

    private static List<ValidationIssue> conflicts(
            List<TemporalBooking> left,
            List<TemporalBooking> right,
            boolean intra
    ) {
        List<ValidationIssue> issues = new ArrayList<>();
        for (int i = 0; i < left.size(); i++) {
            for (int j = intra ? i + 1 : 0; j < right.size(); j++) {
                TemporalBooking first = left.get(i);
                TemporalBooking second = right.get(j);
                if (first.conflicts(second)) {
                    issues.add(critical(
                            "OVERLAP",
                            first.label() + " overlaps activities " + first.activityId() + " and " + second.activityId()
                    ));
                }
            }
        }
        return issues;
    }

    private static ValidationIssue critical(String code, String message) {
        return new ValidationIssue(IssueSeverity.CRITICAL, code, message);
    }
}
