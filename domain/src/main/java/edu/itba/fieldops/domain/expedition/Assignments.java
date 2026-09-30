package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.shared.Stock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Assignments {
    private final List<PersonAssignment> people = new ArrayList<>();
    private final List<VehicleAssignment> vehicles = new ArrayList<>();
    private final List<InstrumentAssignment> instruments = new ArrayList<>();
    private final List<ConsumableAssignment> consumables = new ArrayList<>();

    void add(Assignment assignment) {
        assignment.fileInto(this);
    }

    void remove(Assignment assignment) {
        if (!assignment.withdrawFrom(this)) {
            throw new InvalidAssignment("unknown assignment");
        }
    }

    void file(PersonAssignment person) {
        addOne(people, person);
    }

    void file(VehicleAssignment vehicle) {
        addOne(vehicles, vehicle);
    }

    void file(InstrumentAssignment instrument) {
        addOne(instruments, instrument);
    }

    void file(ConsumableAssignment consumable) {
        addOne(consumables, consumable);
    }

    boolean withdraw(PersonAssignment person) {
        return people.remove(person);
    }

    boolean withdraw(VehicleAssignment vehicle) {
        return vehicles.remove(vehicle);
    }

    boolean withdraw(InstrumentAssignment instrument) {
        return instruments.remove(instrument);
    }

    boolean withdraw(ConsumableAssignment consumable) {
        return consumables.remove(consumable);
    }

    void removeActivity(ActivityId activityId) {
        removeActivity(people, activityId);
        removeActivity(vehicles, activityId);
        removeActivity(instruments, activityId);
        removeActivity(consumables, activityId);
    }

    public List<Assignment> all() {
        List<Assignment> all = new ArrayList<>(people.size() + vehicles.size() + instruments.size() + consumables.size());
        all.addAll(people);
        all.addAll(vehicles);
        all.addAll(instruments);
        all.addAll(consumables);
        return List.copyOf(all);
    }

    public List<PersonAssignment> peopleOf(ActivityId activityId) {
        return ofActivity(people, activityId);
    }

    public List<VehicleAssignment> vehiclesOf(ActivityId activityId) {
        return ofActivity(vehicles, activityId);
    }

    public List<InstrumentAssignment> instrumentsOf(ActivityId activityId) {
        return ofActivity(instruments, activityId);
    }

    public List<ConsumableAssignment> consumables() {
        return List.copyOf(consumables);
    }

    public Map<ConsumableId, Stock> consumption() {
        Map<ConsumableId, Stock> totals = new HashMap<>();
        for (ConsumableAssignment assignment : consumables) {
            totals.merge(assignment.consumableId(), assignment.quantity(), Stock::plus);
        }
        return totals;
    }

    Assignments copy() {
        Assignments copy = new Assignments();
        copy.people.addAll(people);
        copy.vehicles.addAll(vehicles);
        copy.instruments.addAll(instruments);
        copy.consumables.addAll(consumables);
        return copy;
    }

    private static <T> void addOne(List<T> values, T assignment) {
        if (values.contains(assignment)) {
            throw new InvalidAssignment("duplicate assignment");
        }
        values.add(assignment);
    }

    private static void removeActivity(List<? extends Assignment> values, ActivityId activityId) {
        values.removeIf(assignment -> assignment.activityId().equals(activityId));
    }

    private static <T extends Assignment> List<T> ofActivity(List<T> values, ActivityId activityId) {
        return values.stream().filter(assignment -> assignment.activityId().equals(activityId)).toList();
    }
}
