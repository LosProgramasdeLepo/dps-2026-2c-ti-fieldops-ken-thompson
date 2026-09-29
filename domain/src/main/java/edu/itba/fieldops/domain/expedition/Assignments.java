package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.shared.Stock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Assignments {
    private final List<Assignment> values = new ArrayList<>();

    void add(Assignment assignment) {
        if (values.contains(assignment)) {
            throw new InvalidAssignment("duplicate assignment");
        }
        values.add(assignment);
    }

    void remove(Assignment assignment) {
        if (!values.remove(assignment)) {
            throw new InvalidAssignment("unknown assignment");
        }
    }

    void removeActivity(ActivityId activityId) {
        values.removeIf(assignment -> assignment.activityId().equals(activityId));
    }

    public List<Assignment> all() {
        return List.copyOf(values);
    }

    public List<Assignment> of(ActivityId activityId) {
        return values.stream().filter(assignment -> assignment.activityId().equals(activityId)).toList();
    }

    public List<PersonAssignment> peopleOf(ActivityId activityId) {
        return of(activityId).stream().filter(PersonAssignment.class::isInstance).map(PersonAssignment.class::cast).toList();
    }

    public List<VehicleAssignment> vehiclesOf(ActivityId activityId) {
        return of(activityId).stream().filter(VehicleAssignment.class::isInstance).map(VehicleAssignment.class::cast).toList();
    }

    public List<InstrumentAssignment> instrumentsOf(ActivityId activityId) {
        return of(activityId).stream().filter(InstrumentAssignment.class::isInstance).map(InstrumentAssignment.class::cast).toList();
    }

    public List<ConsumableAssignment> consumables() {
        return values.stream().filter(ConsumableAssignment.class::isInstance).map(ConsumableAssignment.class::cast).toList();
    }

    public Map<ConsumableId, Stock> consumption() {
        Map<ConsumableId, Stock> totals = new HashMap<>();
        consumables().forEach(assignment -> totals.merge(assignment.consumableId(), assignment.quantity(), Stock::plus));
        return totals;
    }

    Assignments copy() {
        Assignments copy = new Assignments();
        copy.values.addAll(values);
        return copy;
    }
}
