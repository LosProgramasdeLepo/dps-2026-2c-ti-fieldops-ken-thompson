package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.assessment.IssueSeverity;
import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.catalog.Vehicle;
import edu.itba.fieldops.domain.catalog.Vehicles;
import edu.itba.fieldops.domain.expedition.Assignments;
import edu.itba.fieldops.domain.expedition.PlanningContext;
import edu.itba.fieldops.domain.expedition.VehicleAssignment;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.shared.Capacity;
import edu.itba.fieldops.domain.shared.Passengers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CapacityRule implements ValidationRule {
    @Override
    public List<ValidationIssue> check(PlanningContext context) {
        Assignments assignments = context.plan().assignments();
        List<ValidationIssue> issues = new ArrayList<>();
        for (Activity activity : context.plan().activities()) {
            issueFor(activity, assignments, context.vehicles()).ifPresent(issues::add);
        }
        return issues;
    }

    private static Optional<ValidationIssue> issueFor(Activity activity, Assignments assignments, Vehicles vehicles) {
        List<VehicleAssignment> assigned = assignments.vehiclesOf(activity.id());
        if (assigned.isEmpty()) {
            return Optional.empty();
        }
        List<Capacity> capacities = new ArrayList<>();
        for (VehicleAssignment assignment : assigned) {
            Optional<Vehicle> vehicle = vehicles.vehicle(assignment.vehicleId());
            if (vehicle.isEmpty()) {
                return Optional.empty();
            }
            capacities.add(vehicle.get().capacity());
        }
        Capacity capacity = capacities.stream().reduce(Capacity::plus).orElseThrow();
        Passengers passengers = new Passengers(assignments.peopleOf(activity.id()).size());
        if (capacity.carries(passengers)) {
            return Optional.empty();
        }
        return Optional.of(new ValidationIssue(
                IssueSeverity.WARNING,
                "CAPACITY",
                "activity " + activity.name()
                        + " assigned " + passengers.count()
                        + " people but vehicles can carry " + capacity.seats()
        ));
    }
}
