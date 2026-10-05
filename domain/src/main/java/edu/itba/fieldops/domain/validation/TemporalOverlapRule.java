package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.assessment.IssueSeverity;
import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.catalog.BookableResources;
import edu.itba.fieldops.domain.expedition.PlanningContext;
import edu.itba.fieldops.domain.expedition.TemporalBooking;

import java.util.ArrayList;
import java.util.List;

public final class TemporalOverlapRule implements ValidationRule {
    @Override
    public List<ValidationIssue> check(PlanningContext context) {
        List<TemporalBooking> own = TemporalBooking.of(context.plan());
        List<ValidationIssue> issues = unavailable(own, context.bookable());
        issues.addAll(overlapsWithin(own));
        issues.addAll(overlapsBetween(own, context.occupying().bookings()));
        return issues;
    }

    private static List<ValidationIssue> unavailable(List<TemporalBooking> own, BookableResources resources) {
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
        return issues;
    }

    private static List<ValidationIssue> overlapsWithin(List<TemporalBooking> own) {
        List<ValidationIssue> issues = new ArrayList<>();
        for (int first = 0; first < own.size(); first++) {
            for (int second = first + 1; second < own.size(); second++) {
                if (own.get(first).conflicts(own.get(second))) {
                    issues.add(overlap(own.get(first), own.get(second)));
                }
            }
        }
        return issues;
    }

    private static List<ValidationIssue> overlapsBetween(List<TemporalBooking> own, List<TemporalBooking> occupied) {
        List<ValidationIssue> issues = new ArrayList<>();
        for (TemporalBooking booking : own) {
            for (TemporalBooking other : occupied) {
                if (booking.conflicts(other)) {
                    issues.add(overlap(booking, other));
                }
            }
        }
        return issues;
    }

    private static ValidationIssue overlap(TemporalBooking first, TemporalBooking second) {
        return critical(
                "OVERLAP",
                first.label() + " overlaps activities " + first.activityId() + " and " + second.activityId()
        );
    }

    private static ValidationIssue critical(String code, String message) {
        return new ValidationIssue(IssueSeverity.CRITICAL, code, message);
    }
}
