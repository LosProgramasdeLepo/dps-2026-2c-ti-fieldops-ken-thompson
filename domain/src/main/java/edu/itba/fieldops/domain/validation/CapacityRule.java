package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.assessment.IssueSeverity;
import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.catalog.Catalog;
import edu.itba.fieldops.domain.catalog.Vehicle;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.VehicleAssignment;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.shared.Passengers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CapacityRule implements ValidationRule {
    @Override
    public List<ValidationIssue> check(ValidationContext context) {
        Expedition expedition = context.expedition();
        Catalog catalog = context.catalog();
        List<ValidationIssue> issues = new ArrayList<>();
        for (Activity activity : expedition.itinerary()) {
            issueFor(expedition, catalog, activity).ifPresent(issues::add);
        }
        return issues;
    }

    private static Optional<ValidationIssue> issueFor(Expedition expedition, Catalog catalog, Activity activity) {
        List<VehicleAssignment> assigned = expedition.assignments().vehiclesOf(activity.id());
        for (VehicleAssignment assignment : assigned) {
            if (catalog.vehicle(assignment.vehicleId()).isEmpty()) {
                return Optional.empty();
            }
        }
        if (assigned.isEmpty()) {
            return Optional.empty();
        }
        Passengers capacity = Passengers.ZERO;
        for (VehicleAssignment assignment : assigned) {
            Vehicle vehicle = catalog.vehicle(assignment.vehicleId()).orElseThrow();
            capacity = capacity.plus(vehicle.capacity());
        }
        Passengers passengers = new Passengers(expedition.assignments().peopleOf(activity.id()).size());
        if (capacity.isAtLeast(passengers)) {
            return Optional.empty();
        }
        return Optional.of(new ValidationIssue(
                IssueSeverity.WARNING,
                "CAPACITY",
                "activity " + activity.name()
                        + " assigned " + passengers.count()
                        + " people but vehicles can carry " + capacity.count()
        ));
    }
}
