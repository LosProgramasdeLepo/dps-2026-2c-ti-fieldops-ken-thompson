package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.assessment.IssueSeverity;
import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.InstrumentAssignment;
import edu.itba.fieldops.domain.expedition.PersonAssignment;
import edu.itba.fieldops.domain.expedition.VehicleAssignment;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.ActivityBlock;
import edu.itba.fieldops.domain.itinerary.ItineraryItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ParallelAssignmentRule implements ValidationRule {
    @Override
    public List<ValidationIssue> check(ValidationContext context) {
        List<ValidationIssue> issues = new ArrayList<>();
        for (ItineraryItem item : context.expedition().items()) {
            issues.addAll(in(item, context.expedition()));
        }
        return issues;
    }

    private static List<ValidationIssue> in(ItineraryItem item, Expedition expedition) {
        if (!(item instanceof ActivityBlock block)) {
            return List.of();
        }
        List<ValidationIssue> issues = new ArrayList<>();
        if (block.parallel()) {
            List<List<Activity>> branches = block.parts().stream().map(ItineraryItem::activities).toList();
            for (int left = 0; left < branches.size(); left++) {
                for (int right = left + 1; right < branches.size(); right++) {
                    issues.addAll(shared(expedition, branches.get(left), branches.get(right)));
                }
            }
        }
        for (ItineraryItem part : block.parts()) {
            issues.addAll(in(part, expedition));
        }
        return issues;
    }

    private static List<ValidationIssue> shared(Expedition expedition, List<Activity> left, List<Activity> right) {
        List<ValidationIssue> issues = new ArrayList<>();
        for (PersonId personId : intersection(people(expedition, left), people(expedition, right))) {
            issues.add(conflict("person " + personId));
        }
        for (VehicleId vehicleId : intersection(vehicles(expedition, left), vehicles(expedition, right))) {
            issues.add(conflict("vehicle " + vehicleId));
        }
        for (InstrumentId instrumentId : intersection(instruments(expedition, left), instruments(expedition, right))) {
            issues.add(conflict("instrument " + instrumentId));
        }
        return issues;
    }

    private static Set<PersonId> people(Expedition expedition, List<Activity> branch) {
        Set<PersonId> ids = new HashSet<>();
        for (Activity activity : branch) {
            for (PersonAssignment assignment : expedition.assignments().peopleOf(activity.id())) {
                ids.add(assignment.personId());
            }
        }
        return ids;
    }

    private static Set<VehicleId> vehicles(Expedition expedition, List<Activity> branch) {
        Set<VehicleId> ids = new HashSet<>();
        for (Activity activity : branch) {
            for (VehicleAssignment assignment : expedition.assignments().vehiclesOf(activity.id())) {
                ids.add(assignment.vehicleId());
            }
        }
        return ids;
    }

    private static Set<InstrumentId> instruments(Expedition expedition, List<Activity> branch) {
        Set<InstrumentId> ids = new HashSet<>();
        for (Activity activity : branch) {
            for (InstrumentAssignment assignment : expedition.assignments().instrumentsOf(activity.id())) {
                ids.add(assignment.instrumentId());
            }
        }
        return ids;
    }

    private static <T> Set<T> intersection(Set<T> left, Set<T> right) {
        Set<T> shared = new HashSet<>(left);
        shared.retainAll(right);
        return shared;
    }

    private static ValidationIssue conflict(String resource) {
        return new ValidationIssue(
                IssueSeverity.CRITICAL,
                "PARALLEL",
                resource + " is assigned to parallel branches"
        );
    }
}
