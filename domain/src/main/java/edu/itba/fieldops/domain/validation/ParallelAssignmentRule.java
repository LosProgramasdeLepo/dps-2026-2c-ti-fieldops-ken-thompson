package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.assessment.IssueSeverity;
import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.PlanningContext;
import edu.itba.fieldops.domain.expedition.TemporalBooking;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.ActivityBlock;
import edu.itba.fieldops.domain.itinerary.ItineraryItem;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ParallelAssignmentRule implements ValidationRule {
    @Override
    public List<ValidationIssue> check(PlanningContext context) {
        List<ValidationIssue> issues = new ArrayList<>();
        for (ActivityBlock block : context.plan().blocks()) {
            if (block.arrangement() == ActivityBlock.Arrangement.PARALLEL) {
                issues.addAll(sharedBetweenBranches(context.plan(), block.parts()));
            }
        }
        return issues;
    }

    private static List<ValidationIssue> sharedBetweenBranches(Expedition plan, List<ItineraryItem> branches) {
        List<ValidationIssue> issues = new ArrayList<>();
        for (int left = 0; left < branches.size(); left++) {
            for (int right = left + 1; right < branches.size(); right++) {
                for (String resource : shared(plan, branches.get(left), branches.get(right))) {
                    issues.add(conflict(resource));
                }
            }
        }
        return issues;
    }

    private static Set<String> shared(Expedition plan, ItineraryItem left, ItineraryItem right) {
        List<TemporalBooking> rightBookings = bookings(plan, right);
        Set<String> resources = new LinkedHashSet<>();
        for (TemporalBooking booking : bookings(plan, left)) {
            if (rightBookings.stream().anyMatch(booking::conflicts)) {
                resources.add(booking.label());
            }
        }
        return resources;
    }

    private static List<TemporalBooking> bookings(Expedition plan, ItineraryItem branch) {
        List<ActivityId> activities = branch.activities().stream().map(Activity::id).toList();
        return plan.assignments().bookable().stream()
                .filter(assignment -> activities.contains(assignment.activityId()))
                .map(assignment -> assignment.booking(plan.charter().period()))
                .toList();
    }

    private static ValidationIssue conflict(String resource) {
        return new ValidationIssue(
                IssueSeverity.CRITICAL,
                "PARALLEL",
                resource + " is assigned to parallel branches"
        );
    }
}
