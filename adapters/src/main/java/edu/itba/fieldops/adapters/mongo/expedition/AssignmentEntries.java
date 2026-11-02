package edu.itba.fieldops.adapters.mongo.expedition;

import edu.itba.fieldops.domain.expedition.Assignment;
import edu.itba.fieldops.domain.expedition.ConsumableAssignment;
import edu.itba.fieldops.domain.expedition.InstrumentAssignment;
import edu.itba.fieldops.domain.expedition.PersonAssignment;
import edu.itba.fieldops.domain.expedition.VehicleAssignment;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.shared.Consumption;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

record AssignmentEntries(
        List<Booking> people,
        List<Booking> vehicles,
        List<Booking> instruments,
        List<ConsumableEntry> consumables
) {
    static AssignmentEntries of(List<Assignment> assignments) {
        AssignmentEntries entries = new AssignmentEntries(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        assignments.forEach(entries::add);
        return entries;
    }

    List<Assignment> toDomain() {
        return Stream.of(
                people.stream().<Assignment>map(entry -> new PersonAssignment(entry.activity(), new PersonId(entry.resource()))),
                vehicles.stream().<Assignment>map(entry -> new VehicleAssignment(entry.activity(), new VehicleId(entry.resource()))),
                instruments.stream().<Assignment>map(entry -> new InstrumentAssignment(entry.activity(), new InstrumentId(entry.resource()))),
                consumables.stream().<Assignment>map(ConsumableEntry::toDomain)
        ).flatMap(assignment -> assignment).toList();
    }

    private void add(Assignment assignment) {
        switch (assignment) {
            case PersonAssignment person -> people.add(Booking.of(person.activityId(), person.personId().value()));
            case VehicleAssignment vehicle -> vehicles.add(Booking.of(vehicle.activityId(), vehicle.vehicleId().value()));
            case InstrumentAssignment instrument -> instruments.add(Booking.of(instrument.activityId(), instrument.instrumentId().value()));
            case ConsumableAssignment consumable -> consumables.add(ConsumableEntry.of(consumable));
        }
    }

    record Booking(String activityId, String resourceId) {
        static Booking of(ActivityId activityId, UUID resourceId) {
            return new Booking(activityId.value().toString(), resourceId.toString());
        }

        ActivityId activity() {
            return new ActivityId(UUID.fromString(activityId));
        }

        UUID resource() {
            return UUID.fromString(resourceId);
        }
    }

    record ConsumableEntry(String activityId, String consumableId, int quantity) {
        static ConsumableEntry of(ConsumableAssignment assignment) {
            return new ConsumableEntry(
                    assignment.activityId().value().toString(),
                    assignment.consumableId().value().toString(),
                    assignment.quantity().amount()
            );
        }

        ConsumableAssignment toDomain() {
            return new ConsumableAssignment(
                    new ActivityId(UUID.fromString(activityId)),
                    new ConsumableId(UUID.fromString(consumableId)),
                    new Consumption(quantity)
            );
        }
    }
}
